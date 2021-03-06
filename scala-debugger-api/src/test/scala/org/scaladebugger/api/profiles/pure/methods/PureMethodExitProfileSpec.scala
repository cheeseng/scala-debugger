package org.scaladebugger.api.profiles.pure.methods
import acyclic.file

import com.sun.jdi.event.Event
import org.scaladebugger.api.lowlevel.events.EventManager
import org.scaladebugger.api.lowlevel.events.EventType.MethodExitEventType
import org.scaladebugger.api.lowlevel.events.data.JDIEventDataResult
import org.scaladebugger.api.lowlevel.events.filters.{MethodNameFilter, UniqueIdPropertyFilter}
import org.scaladebugger.api.lowlevel.methods.{MethodExitManager, MethodExitRequestInfo, PendingMethodExitSupportLike}
import org.scaladebugger.api.lowlevel.requests.JDIRequestArgument
import org.scaladebugger.api.lowlevel.requests.properties.UniqueIdProperty
import org.scaladebugger.api.pipelines.Pipeline
import org.scaladebugger.api.profiles.Constants
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import test.JDIMockHelpers

import scala.util.{Failure, Success}

class PureMethodExitProfileSpec extends FunSpec with Matchers
with ParallelTestExecution with MockFactory with JDIMockHelpers
{
  private val TestRequestId = java.util.UUID.randomUUID().toString
  private val mockMethodExitManager = mock[MethodExitManager]
  private val mockEventManager = mock[EventManager]

  private val pureMethodExitProfile = new Object with PureMethodExitProfile {
    private var requestId: String = _
    def setRequestId(requestId: String): Unit = this.requestId = requestId

    // NOTE: If we set a specific request id, return that, otherwise use the
    //       default behavior
    override protected def newMethodExitRequestId(): String =
      if (requestId != null) requestId else super.newMethodExitRequestId()

    override protected val methodExitManager = mockMethodExitManager
    override protected val eventManager: EventManager = mockEventManager
  }

  describe("PureMethodExitProfile") {
    describe("#methodExitRequests") {
      it("should include all active requests") {
        val expected = Seq(
          MethodExitRequestInfo(
            TestRequestId,
            "some.class.name",
            "someMethodName"
          )
        )

        val mockMethodExitManager = mock[PendingMethodExitSupportLike]
        val pureMethodExitProfile = new Object with PureMethodExitProfile {
          override protected val methodExitManager = mockMethodExitManager
          override protected val eventManager: EventManager = mockEventManager
        }

        (mockMethodExitManager.methodExitRequestList _).expects()
          .returning(expected).once()

        (mockMethodExitManager.pendingMethodExitRequests _).expects()
          .returning(Nil).once()

        val actual = pureMethodExitProfile.methodExitRequests

        actual should be (expected)
      }

      it("should include pending requests if supported") {
        val expected = Seq(
          MethodExitRequestInfo(
            TestRequestId,
            "some.class.name",
            "someMethodName"
          )
        )

        val mockMethodExitManager = mock[PendingMethodExitSupportLike]
        val pureMethodExitProfile = new Object with PureMethodExitProfile {
          override protected val methodExitManager = mockMethodExitManager
          override protected val eventManager: EventManager = mockEventManager
        }

        (mockMethodExitManager.methodExitRequestList _).expects()
          .returning(Nil).once()

        (mockMethodExitManager.pendingMethodExitRequests _).expects()
          .returning(expected).once()

        val actual = pureMethodExitProfile.methodExitRequests

        actual should be (expected)
      }

      it("should only include active requests if pending unsupported") {
        val expected = Seq(
          MethodExitRequestInfo(
            TestRequestId,
            "some.class.name",
            "someMethodName"
          )
        )

        (mockMethodExitManager.methodExitRequestList _).expects()
          .returning(expected).once()

        val actual = pureMethodExitProfile.methodExitRequests

        actual should be (expected)
      }
    }

    describe("#onMethodExitWithData") {
      it("should create a new request if one has not be made yet") {
        val className = "some.class.name"
        val methodName = "someMethodName"
        val arguments = Seq(mock[JDIRequestArgument])

        val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
        val uniqueIdPropertyFilter = UniqueIdPropertyFilter(id = TestRequestId)
        val eventArguments = Seq(
          uniqueIdPropertyFilter,
          MethodNameFilter(methodName)
        )

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureMethodExitProfile.setRequestId(TestRequestId)

        inSequence {
          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockMethodExitManager.hasMethodExitRequest _)
            .expects(className, methodName)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockMethodExitManager.createMethodExitRequestWithId _).expects(
            TestRequestId,
            className,
            methodName,
            uniqueIdProperty +: arguments
          ).returning(Success("")).once()

          (mockEventManager.addEventDataStream _)
            .expects(MethodExitEventType, eventArguments)
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )
      }

      it("should capture exceptions thrown when creating the request") {
        val expected = Failure(new Throwable)
        val className = "some.class.name"
        val methodName = "someMethodName"
        val arguments = Seq(mock[JDIRequestArgument])

        val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureMethodExitProfile.setRequestId(TestRequestId)

        inSequence {
          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockMethodExitManager.hasMethodExitRequest _)
            .expects(className, methodName)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockMethodExitManager.createMethodExitRequestWithId _).expects(
            TestRequestId,
            className,
            methodName,
            uniqueIdProperty +: arguments
          ).throwing(expected.failed.get).once()
        }

        val actual = pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )

        actual should be (expected)
      }

      it("should create a new request if the previous one was removed") {
        val className = "some.class.name"
        val methodName = "someMethodName"
        val arguments = Seq(mock[JDIRequestArgument])

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureMethodExitProfile.setRequestId(TestRequestId)

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureMethodExitProfile.setRequestId(TestRequestId)

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId)
          val eventArguments = Seq(
            uniqueIdPropertyFilter,
            MethodNameFilter(methodName)
          )

          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockMethodExitManager.hasMethodExitRequest _)
            .expects(className, methodName)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockMethodExitManager.createMethodExitRequestWithId _).expects(
            TestRequestId,
            className,
            methodName,
            uniqueIdProperty +: arguments
          ).returning(Success("")).once()

          (mockEventManager.addEventDataStream _)
            .expects(MethodExitEventType, eventArguments)
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureMethodExitProfile.setRequestId(TestRequestId + "other")

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId + "other")
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId + "other")
          val eventArguments = Seq(
            uniqueIdPropertyFilter,
            MethodNameFilter(methodName)
          )

          // Return false this time to indicate that the methodExit request
          // was removed some time between the two calls
          (mockMethodExitManager.hasMethodExitRequest _)
            .expects(className, methodName)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockMethodExitManager.createMethodExitRequestWithId _).expects(
            TestRequestId + "other",
            className,
            methodName,
            uniqueIdProperty +: arguments
          ).returning(Success("")).once()

          (mockEventManager.addEventDataStream _)
            .expects(MethodExitEventType, eventArguments)
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )
      }

      it("should not create a new request if the previous one still exists") {
        val className = "some.class.name"
        val methodName = "someMethodName"
        val arguments = Seq(mock[JDIRequestArgument])

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureMethodExitProfile.setRequestId(TestRequestId)

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId)
          val eventArguments = Seq(
            uniqueIdPropertyFilter,
            MethodNameFilter(methodName)
          )

          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockMethodExitManager.hasMethodExitRequest _)
            .expects(className, methodName)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockMethodExitManager.createMethodExitRequestWithId _).expects(
            TestRequestId,
            className,
            methodName,
            uniqueIdProperty +: arguments
          ).returning(Success("")).once()

          (mockEventManager.addEventDataStream _)
            .expects(MethodExitEventType, eventArguments)
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureMethodExitProfile.setRequestId(TestRequestId + "other")

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId)
          val eventArguments = Seq(
            uniqueIdPropertyFilter,
            MethodNameFilter(methodName)
          )

          // Return true to indicate that we do still have the request
          (mockMethodExitManager.hasMethodExitRequest _)
            .expects(className, methodName)
            .returning(true).once()

          (mockEventManager.addEventDataStream _)
            .expects(MethodExitEventType, eventArguments)
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )
      }

      it("should create a new request for different input") {
        val className = "some.class.name"
        val methodName = "someMethodName"
        val arguments = Seq(mock[JDIRequestArgument])

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureMethodExitProfile.setRequestId(TestRequestId)

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureMethodExitProfile.setRequestId(TestRequestId)

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId)
          val eventArguments = Seq(
            uniqueIdPropertyFilter,
            MethodNameFilter(methodName)
          )

          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockMethodExitManager.hasMethodExitRequest _)
            .expects(className, methodName)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockMethodExitManager.createMethodExitRequestWithId _).expects(
            TestRequestId,
            className,
            methodName,
            uniqueIdProperty +: arguments
          ).returning(Success("")).once()

          (mockEventManager.addEventDataStream _)
            .expects(MethodExitEventType, eventArguments)
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )

        inSequence {
          // Set a known test id so we can validate the unique property is added
          import scala.language.reflectiveCalls
          pureMethodExitProfile.setRequestId(TestRequestId + "other")

          val uniqueIdProperty = UniqueIdProperty(id = TestRequestId + "other")
          val uniqueIdPropertyFilter =
            UniqueIdPropertyFilter(id = TestRequestId + "other")
          val eventArguments = Seq(
            uniqueIdPropertyFilter,
            MethodNameFilter(methodName + 1)
          )

          // Memoized request function first checks to make sure the cache
          // has not been invalidated underneath (first call will always be
          // false since we have never created the request)
          (mockMethodExitManager.hasMethodExitRequest _)
            .expects(className, methodName + 1)
            .returning(false).once()

          // NOTE: Expect the request to be created with a unique id
          (mockMethodExitManager.createMethodExitRequestWithId _).expects(
            TestRequestId + "other",
            className,
            methodName + 1,
            uniqueIdProperty +: arguments
          ).returning(Success("")).once()

          (mockEventManager.addEventDataStream _)
            .expects(MethodExitEventType, eventArguments)
            .returning(Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            )).once()
        }

        pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName + 1,
          arguments: _*
        )
      }

      it("should remove the underlying request if all pipelines are closed") {
        val className = "some.class.name"
        val methodName = "someMethodName"
        val arguments = Seq(mock[JDIRequestArgument])

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureMethodExitProfile.setRequestId(TestRequestId)

        inSequence {
          val eventHandlerIds = Seq("a", "b")
          inAnyOrder {
            val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
            val uniqueIdPropertyFilter =
              UniqueIdPropertyFilter(id = TestRequestId)

            val eventArguments = Seq(
              uniqueIdPropertyFilter,
              MethodNameFilter(methodName)
            )

            // Memoized request function first checks to make sure the cache
            // has not been invalidated underneath (first call will always be
            // empty since we have never created the request)
            (mockMethodExitManager.hasMethodExitRequest _)
              .expects(className, methodName)
              .returning(false).once()
            (mockMethodExitManager.hasMethodExitRequest _)
              .expects(className, methodName)
              .returning(true).once()

            // NOTE: Expect the request to be created with a unique id
            (mockMethodExitManager.createMethodExitRequestWithId _).expects(
              TestRequestId,
              className,
              methodName,
              uniqueIdProperty +: arguments
            ).returning(Success("")).once()

            // NOTE: Pipeline adds an event handler id to its metadata
            def newEventPipeline(id: String) = Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            ).withMetadata(Map(EventManager.EventHandlerIdMetadataField -> id))

            eventHandlerIds.foreach(id => {
              (mockEventManager.addEventDataStream _)
                .expects(MethodExitEventType, eventArguments)
                .returning(newEventPipeline(id)).once()
            })
          }

          (mockMethodExitManager.removeMethodExitRequestWithId _)
            .expects(TestRequestId).once()
          eventHandlerIds.foreach(id => {
            (mockEventManager.removeEventHandler _).expects(id).once()
          })
        }

        val p1 = pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )
        val p2 = pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )

        p1.foreach(_.close())
        p2.foreach(_.close())
      }

      it("should remove the underlying request if close data says to do so") {
        val className = "some.class.name"
        val methodName = "someMethodName"
        val arguments = Seq(mock[JDIRequestArgument])

        // Set a known test id so we can validate the unique property is added
        import scala.language.reflectiveCalls
        pureMethodExitProfile.setRequestId(TestRequestId)

        inSequence {
          val eventHandlerIds = Seq("a", "b")
          inAnyOrder {
            val uniqueIdProperty = UniqueIdProperty(id = TestRequestId)
            val uniqueIdPropertyFilter =
              UniqueIdPropertyFilter(id = TestRequestId)

            val eventArguments = Seq(
              uniqueIdPropertyFilter,
              MethodNameFilter(methodName)
            )

            // Memoized request function first checks to make sure the cache
            // has not been invalidated underneath (first call will always be
            // empty since we have never created the request)
            (mockMethodExitManager.hasMethodExitRequest _)
              .expects(className, methodName)
              .returning(false).once()
            (mockMethodExitManager.hasMethodExitRequest _)
              .expects(className, methodName)
              .returning(true).once()

            // NOTE: Expect the request to be created with a unique id
            (mockMethodExitManager.createMethodExitRequestWithId _).expects(
              TestRequestId,
              className,
              methodName,
              uniqueIdProperty +: arguments
            ).returning(Success("")).once()

            // NOTE: Pipeline adds an event handler id to its metadata
            def newEventPipeline(id: String) = Pipeline.newPipeline(
              classOf[(Event, Seq[JDIEventDataResult])]
            ).withMetadata(Map(EventManager.EventHandlerIdMetadataField -> id))

            eventHandlerIds.foreach(id => {
              (mockEventManager.addEventDataStream _)
                .expects(MethodExitEventType, eventArguments)
                .returning(newEventPipeline(id)).once()
            })
          }

          (mockMethodExitManager.removeMethodExitRequestWithId _)
            .expects(TestRequestId).once()
          eventHandlerIds.foreach(id => {
            (mockEventManager.removeEventHandler _).expects(id).once()
          })
        }

        val p1 = pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )
        val p2 = pureMethodExitProfile.onMethodExitWithData(
          className,
          methodName,
          arguments: _*
        )

        p1.foreach(_.close(now = true, data = Constants.CloseRemoveAll))
      }
    }
  }
}

