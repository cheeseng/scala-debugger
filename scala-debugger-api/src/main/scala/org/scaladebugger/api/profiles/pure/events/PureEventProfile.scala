package org.scaladebugger.api.profiles.pure.events
import acyclic.file

import org.scaladebugger.api.lowlevel.JDIArgument
import org.scaladebugger.api.lowlevel.events.{PendingEventHandlerSupportLike, EventHandlerInfo, EventManager}
import org.scaladebugger.api.lowlevel.events.EventType.EventType
import org.scaladebugger.api.lowlevel.utils.JDIArgumentGroup
import org.scaladebugger.api.pipelines.Pipeline
import org.scaladebugger.api.pipelines.Pipeline.IdentityPipeline
import org.scaladebugger.api.profiles.traits.events.EventProfile

import scala.util.Try

/**
 * Represents a pure profile for events that adds no extra logic on
 * top of the standard JDI.
 */
trait PureEventProfile extends EventProfile {
  protected val eventManager: EventManager

  /**
   * Constructs a stream of events for the specified event type.
   *
   * @param eventType The type of event to stream
   * @param extraArguments The additional JDI arguments to provide
   *
   * @return The stream of events and any retrieved data based on
   *         requests from extra arguments
   */
  override def onEventWithData(
    eventType: EventType,
    extraArguments: JDIArgument*
  ): Try[IdentityPipeline[EventAndData]] = Try {
    val JDIArgumentGroup(_, eArgs, _) = JDIArgumentGroup(extraArguments: _*)
    eventManager.addEventDataStream(eventType, eArgs: _*)
  }

  /**
   * Retrieves the collection of active event handlers.
   *
   * @return The collection of information on event handlers
   */
  override def eventHandlers: Seq[EventHandlerInfo] = {
    eventManager.getAllEventHandlerInfo ++ (eventManager match {
      case p: PendingEventHandlerSupportLike  => p.pendingEventHandlers
      case _                                  => Nil
    })
  }
}
