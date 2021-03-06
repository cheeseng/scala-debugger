package org.scaladebugger.api.lowlevel.watchpoints
import acyclic.file

import com.sun.jdi.{ReferenceType, VirtualMachine, Field}
import com.sun.jdi.request.{EventRequest, EventRequestManager, AccessWatchpointRequest}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import org.scaladebugger.api.lowlevel.classes.ClassManager
import test.JDIMockHelpers
import scala.collection.JavaConverters._

import scala.util.{Failure, Success}

class StandardAccessWatchpointManagerSpec extends FunSpec with Matchers
  with ParallelTestExecution with MockFactory with JDIMockHelpers
{
  private val TestRequestId = java.util.UUID.randomUUID().toString
  private val mockEventRequestManager = mock[EventRequestManager]
  private val mockClassManager = mock[ClassManager]

  private val accessWatchpointManager = new StandardAccessWatchpointManager(
    mockEventRequestManager,
    mockClassManager
  ) {
    override protected def newRequestId(): String = TestRequestId
  }

  describe("StandardAccessWatchpointManager") {
    describe("#accessWatchpointRequestListById") {
      it("should contain all access watchpoint requests in the form of field stored in the manager") {
        val requestIds = Seq(TestRequestId, TestRequestId + 1, TestRequestId + 2)
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        requestIds.foreach { case requestId =>
          val stubField = createFieldStub(testFieldName)
          val mockReferenceType = mock[ReferenceType]
          (mockClassManager.allClasses _).expects()
            .returning(Seq(mockReferenceType)).once()
          (mockReferenceType.name _).expects().returning(testClassName).once()
          (mockReferenceType.allFields _).expects()
            .returning(Seq(stubField).asJava).once()

          (mockEventRequestManager.createAccessWatchpointRequest _)
            .expects(stubField)
            .returning(stub[AccessWatchpointRequest]).once()
          accessWatchpointManager.createAccessWatchpointRequestWithId(
            requestId,
            testClassName,
            testFieldName
          )
        }

        accessWatchpointManager.accessWatchpointRequestListById should
          contain theSameElementsAs (requestIds)
      }
    }

    describe("#accessWatchpointRequestList") {
      it("should contain all access watchpoint requests in the form of field stored in the manager") {
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"
        val expected = Seq(
          AccessWatchpointRequestInfo(TestRequestId, testClassName, testFieldName),
          AccessWatchpointRequestInfo(TestRequestId + 1, testClassName, testFieldName + 1),
          AccessWatchpointRequestInfo(TestRequestId + 2, testClassName, testFieldName + 2)
        )

        // NOTE: Must create a new accessWatchpoint manager that does NOT override the
        //       request id to always be the same since we do not allow
        //       duplicates of the test id when storing it
        val accessWatchpointManager = new StandardAccessWatchpointManager(
          mockEventRequestManager,
          mockClassManager
        )

        expected.foreach { case AccessWatchpointRequestInfo(requestId, className, fieldName, _) =>
          val stubField = createFieldStub(fieldName)
          val mockReferenceType = mock[ReferenceType]
          (mockClassManager.allClasses _).expects()
            .returning(Seq(mockReferenceType)).once()
          (mockReferenceType.name _).expects().returning(className).once()
          (mockReferenceType.allFields _).expects()
            .returning(Seq(stubField).asJava).once()

          (mockEventRequestManager.createAccessWatchpointRequest _)
            .expects(stubField)
            .returning(stub[AccessWatchpointRequest]).once()
          accessWatchpointManager.createAccessWatchpointRequestWithId(
            requestId,
            className,
            fieldName
          )
        }

        val actual = accessWatchpointManager.accessWatchpointRequestList
        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#createAccessWatchpointRequestWithId") {
      it("should create the access watchpoint request using the provided id") {
        val expected = Success(java.util.UUID.randomUUID().toString)
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        val mockAccessWatchpointRequest = mock[AccessWatchpointRequest]
        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .returning(mockAccessWatchpointRequest).once()

        (mockAccessWatchpointRequest.setSuspendPolicy _)
          .expects(EventRequest.SUSPEND_EVENT_THREAD).once()
        (mockAccessWatchpointRequest.setEnabled _).expects(true).once()

        val actual = accessWatchpointManager.createAccessWatchpointRequestWithId(
          expected.get,
          testClassName,
          testFieldName
        )
        actual should be(expected)
      }

      it("should create the access watchpoint request and return Success(id)") {
        val expected = Success(TestRequestId)
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        val mockAccessWatchpointRequest = mock[AccessWatchpointRequest]
        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .returning(mockAccessWatchpointRequest).once()

        (mockAccessWatchpointRequest.setSuspendPolicy _)
          .expects(EventRequest.SUSPEND_EVENT_THREAD).once()
        (mockAccessWatchpointRequest.setEnabled _).expects(true).once()

        val actual = accessWatchpointManager.createAccessWatchpointRequestWithId(
          expected.get,
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }

      it("should return the exception if failed to create the access watchpoint request") {
        val expected = Failure(new Throwable)
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .throwing(expected.failed.get).once()

        val actual = accessWatchpointManager.createAccessWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }

      it("should return a failure if the class of the field was not found") {
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"
        val expected = Failure(NoFieldFound(testClassName, testFieldName))

        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()

        // Provide reference types with different names so there is no match
        (mockReferenceType.name _).expects().returning(testClassName + 1).once()

        val actual = accessWatchpointManager.createAccessWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }

      it("should return a failure if the field with the specified name was not found") {
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"
        val expected = Failure(NoFieldFound(testClassName, testFieldName))

        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()

        // Provide fields with different names so there is no match
        val stubField = createFieldStub(testFieldName + 1)
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        val actual = accessWatchpointManager.createAccessWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }
    }

    describe("#hasAccessWatchpointRequestWithId") {
      it("should return true if it exists") {
        val expected = true
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .returning(stub[AccessWatchpointRequest]).once()

        accessWatchpointManager.createAccessWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )

        val actual = accessWatchpointManager.hasAccessWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }

      it("should return false if it does not exist") {
        val expected = false

        val actual = accessWatchpointManager.hasAccessWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }
    }

    describe("#hasAccessWatchpointRequest") {
      it("should return true if it exists") {
        val expected = true
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .returning(stub[AccessWatchpointRequest]).once()

        accessWatchpointManager.createAccessWatchpointRequest(
          testClassName,
          testFieldName
        )

        val actual = accessWatchpointManager.hasAccessWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }

      it("should return false if it does not exist") {
        val expected = false
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val actual = accessWatchpointManager.hasAccessWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }
    }

    describe("#getAccessWatchpointRequestWithId") {
      it("should return Some(AccessWatchpointRequest) if found") {
        val expected = Some(stub[AccessWatchpointRequest])
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .returning(expected.get).once()

        accessWatchpointManager.createAccessWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )

        val actual = accessWatchpointManager.getAccessWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }

      it("should return None if not found") {
        val expected = None

        val actual = accessWatchpointManager.getAccessWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }
    }

    describe("#getAccessWatchpointRequest") {
      it("should return Some(Seq(AccessWatchpointRequest)) if found") {
        val expected = Seq(stub[AccessWatchpointRequest])
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .returning(expected.head).once()

        accessWatchpointManager.createAccessWatchpointRequest(
          testClassName,
          testFieldName
        )

        val actual = accessWatchpointManager.getAccessWatchpointRequest(
          testClassName,
          testFieldName
        ).get
        actual should contain theSameElementsAs (expected)
      }

      it("should return None if not found") {
        val expected = None
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val actual = accessWatchpointManager.getAccessWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }
    }

    describe("#removeAccessWatchpointRequestWithId") {
      it("should return true if the access watchpoint request was removed") {
        val expected = true
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"
        val stubRequest = stub[AccessWatchpointRequest]

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .returning(stubRequest).once()

        accessWatchpointManager.createAccessWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )

        (mockEventRequestManager.deleteEventRequest _)
          .expects(stubRequest).once()

        val actual = accessWatchpointManager.removeAccessWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }

      it("should return false if the access watchpoint request was not removed") {
        val expected = false

        val actual = accessWatchpointManager.removeAccessWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }
    }

    describe("#removeAccessWatchpointRequest") {
      it("should return true if the access watchpoint request was removed") {
        val expected = true
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"
        val stubRequest = stub[AccessWatchpointRequest]

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createAccessWatchpointRequest _)
          .expects(stubField)
          .returning(stubRequest).once()

        accessWatchpointManager.createAccessWatchpointRequest(
          testClassName,
          testFieldName
        )

        (mockEventRequestManager.deleteEventRequest _)
          .expects(stubRequest).once()

        val actual = accessWatchpointManager.removeAccessWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }

      it("should return false if the access watchpoint request was not removed") {
        val expected = false
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val actual = accessWatchpointManager.removeAccessWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }
    }
  }
}
