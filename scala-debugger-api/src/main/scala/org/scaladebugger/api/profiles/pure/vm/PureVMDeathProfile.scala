package org.scaladebugger.api.profiles.pure.vm
import acyclic.file

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.sun.jdi.event.VMDeathEvent
import org.scaladebugger.api.lowlevel.JDIArgument
import org.scaladebugger.api.lowlevel.events.{EventManager, JDIEventArgument}
import org.scaladebugger.api.lowlevel.events.filters.UniqueIdPropertyFilter
import org.scaladebugger.api.lowlevel.requests.JDIRequestArgument
import org.scaladebugger.api.lowlevel.requests.properties.UniqueIdProperty
import org.scaladebugger.api.lowlevel.utils.JDIArgumentGroup
import org.scaladebugger.api.lowlevel.vm._
import org.scaladebugger.api.pipelines.Pipeline
import org.scaladebugger.api.pipelines.Pipeline.IdentityPipeline
import org.scaladebugger.api.profiles.traits.vm.VMDeathProfile
import org.scaladebugger.api.utils.{MultiMap, Memoization}
import org.scaladebugger.api.lowlevel.events.EventType.VMDeathEventType
import org.scaladebugger.api.profiles.Constants._
import scala.collection.JavaConverters._
import scala.util.Try

/**
 * Represents a pure profile for vm death events that adds no
 * extra logic on top of the standard JDI.
 */
trait PureVMDeathProfile extends VMDeathProfile {
  protected val vmDeathManager: VMDeathManager
  protected val eventManager: EventManager

  /**
   * Contains a mapping of request ids to associated event handler ids.
   */
  private val pipelineRequestEventIds = new MultiMap[String, String]

  /**
   * Contains mapping from input to a counter indicating how many pipelines
   * are currently active for the input.
   */
  private val pipelineCounter = new ConcurrentHashMap[
    Seq[JDIArgument],
    AtomicInteger
  ]().asScala

  /**
   * Retrieves the collection of active and pending vm death requests.
   *
   * @return The collection of information on vm death requests
   */
  override def vmDeathRequests: Seq[VMDeathRequestInfo] = {
    val activeRequests = vmDeathManager.vmDeathRequestList.flatMap(
      vmDeathManager.getVMDeathRequestInfo
    )

    activeRequests ++ (vmDeathManager match {
      case p: PendingVMDeathSupportLike => p.pendingVMDeathRequests
      case _                            => Nil
    })
  }

  /**
   * Constructs a stream of vm death events.
   *
   * @param extraArguments The additional JDI arguments to provide
   *
   * @return The stream of vm death events and any retrieved data based on
   *         requests from extra arguments
   */
  override def onVMDeathWithData(
    extraArguments: JDIArgument*
  ): Try[IdentityPipeline[VMDeathEventAndData]] = Try {
    val JDIArgumentGroup(rArgs, eArgs, _) = JDIArgumentGroup(extraArguments: _*)
    val requestId = newVMDeathRequest(rArgs)
    newVMDeathPipeline(requestId, eArgs)
  }

  /**
   * Creates a new vm death request using the given arguments. The request
   * is memoized, meaning that the same request will be returned for the same
   * arguments. The memoized result will be thrown out if the underlying
   * request storage indicates that the request has been removed.
   *
   * @return The id of the created vm death request
   */
  protected val newVMDeathRequest = {
    type Input = (Seq[JDIRequestArgument])
    type Key = (Seq[JDIRequestArgument])
    type Output = String

    new Memoization[Input, Key, Output](
      memoFunc = (input: Input) => {
        val requestId = newVMDeathRequestId()
        val args = UniqueIdProperty(id = requestId) +: input

        vmDeathManager.createVMDeathRequestWithId(
          requestId,
          args: _*
        ).get

        requestId
      },
      cacheInvalidFunc = (key: Key) => {
        // TODO: Remove hard-coded filtering out of UniqueIdProperty,
        //       which shows up in saved arguments since it is passed in
        //       during the memoization above
        !vmDeathManager.vmDeathRequestList
          .flatMap(vmDeathManager.getVMDeathRequestInfo)
          .map(_.extraArguments)
          .map(_.filterNot(_.isInstanceOf[UniqueIdProperty]))
          .contains(key)
      }
    )
  }

  /**
   * Creates a new pipeline of vm death events and data using the given
   * arguments. The pipeline is NOT memoized; therefore, each call creates a
   * new pipeline with a new underlying event handler feeding the pipeline.
   * This means that the pipeline needs to be properly closed to remove the
   * event handler.
   *
   * @param requestId The id of the request whose events to stream through the
   *                  new pipeline
   * @param args The additional event arguments to provide to the event handler
   *             feeding the new pipeline
   * @return The new vm death event and data pipeline
   */
  protected def newVMDeathPipeline(
    requestId: String,
    args: Seq[JDIEventArgument]
  ): IdentityPipeline[VMDeathEventAndData] = {
    val eArgsWithFilter = UniqueIdPropertyFilter(id = requestId) +: args
    val newPipeline = eventManager
      .addEventDataStream(VMDeathEventType, eArgsWithFilter: _*)
      .map(t => (t._1.asInstanceOf[VMDeathEvent], t._2))
      .noop()

    // Create a companion pipeline who, when closed, checks to see if there
    // are no more pipelines for the given request and, if so, removes the
    // request as well
    val closePipeline = Pipeline.newPipeline(
      classOf[VMDeathEventAndData],
      newVMDeathPipelineCloseFunc(requestId, args)
    )

    // Increment the counter for open pipelines
    pipelineCounter
      .getOrElseUpdate(args, new AtomicInteger(0))
      .incrementAndGet()

    val combinedPipeline = newPipeline.unionOutput(closePipeline)

    // Store the new event handler id as associated with the current request
    pipelineRequestEventIds.put(
      requestId,
      combinedPipeline.currentMetadata(
        EventManager.EventHandlerIdMetadataField
      ).asInstanceOf[String]
    )

    combinedPipeline
  }

  /**
   * Creates a new function used for closing generated pipelines.
   *
   * @param requestId The id of the request
   * @param args The arguments associated with the request
   *
   * @return The new function for closing the pipeline
   */
  protected def newVMDeathPipelineCloseFunc(
    requestId: String,
    args: Seq[JDIEventArgument]
  ): (Option[Any]) => Unit = (data: Option[Any]) => {
    val pCounter = pipelineCounter(args)

    val totalPipelinesRemaining = pCounter.decrementAndGet()

    if (totalPipelinesRemaining == 0 || data.exists(_ == CloseRemoveAll)) {
      vmDeathManager.removeVMDeathRequest(requestId)
      pipelineRequestEventIds.remove(requestId).foreach(
        _.foreach(eventManager.removeEventHandler)
      )
      pCounter.set(0)
    }
  }

  /**
   * Used to generate new request ids to capture request/event matches.
   *
   * @return The new id as a string
   */
  protected def newVMDeathRequestId(): String =
    java.util.UUID.randomUUID().toString
}
