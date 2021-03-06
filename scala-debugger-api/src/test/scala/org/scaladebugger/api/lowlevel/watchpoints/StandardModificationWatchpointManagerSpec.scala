package org.scaladebugger.api.lowlevel.watchpoints
import acyclic.file

import com.sun.jdi.request.{ModificationWatchpointRequest, EventRequest, EventRequestManager}
import com.sun.jdi.{ReferenceType, VirtualMachine}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import org.scaladebugger.api.lowlevel.classes.ClassManager
import test.JDIMockHelpers

import scala.collection.JavaConverters._
import scala.util.{Failure, Success}

class StandardModificationWatchpointManagerSpec extends FunSpec with Matchers
  with ParallelTestExecution with MockFactory with JDIMockHelpers
{
  private val TestRequestId = java.util.UUID.randomUUID().toString
  private val mockEventRequestManager = mock[EventRequestManager]
  private val mockClassManager = mock[ClassManager]

  private val modificationWatchpointManager = new StandardModificationWatchpointManager(
    mockEventRequestManager,
    mockClassManager
  ) {
    override protected def newRequestId(): String = TestRequestId
  }

  describe("StandardModificationWatchpointManager") {
    describe("#modificationWatchpointRequestListById") {
      it("should contain all modification watchpoint requests in the form of field stored in the manager") {
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

          (mockEventRequestManager.createModificationWatchpointRequest _)
            .expects(stubField)
            .returning(stub[ModificationWatchpointRequest]).once()
          modificationWatchpointManager.createModificationWatchpointRequestWithId(
            requestId,
            testClassName,
            testFieldName
          )
        }

        modificationWatchpointManager.modificationWatchpointRequestListById should
          contain theSameElementsAs (requestIds)
      }
    }

    describe("#modificationWatchpointRequestList") {
      it("should contain all modification watchpoint requests in the form of field stored in the manager") {
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"
        val expected = Seq(
          ModificationWatchpointRequestInfo(TestRequestId, testClassName, testFieldName),
          ModificationWatchpointRequestInfo(TestRequestId + 1, testClassName, testFieldName + 1),
          ModificationWatchpointRequestInfo(TestRequestId + 2, testClassName, testFieldName + 2)
        )

        // NOTE: Must create a new modificationWatchpoint manager that does NOT override the
        //       request id to always be the same since we do not allow
        //       duplicates of the test id when storing it
        val modificationWatchpointManager = new StandardModificationWatchpointManager(
          mockEventRequestManager,
          mockClassManager
        )

        expected.foreach { case ModificationWatchpointRequestInfo(requestId, className, fieldName, _) =>
          val stubField = createFieldStub(fieldName)
          val mockReferenceType = mock[ReferenceType]
          (mockClassManager.allClasses _).expects()
            .returning(Seq(mockReferenceType)).once()
          (mockReferenceType.name _).expects().returning(className).once()
          (mockReferenceType.allFields _).expects()
            .returning(Seq(stubField).asJava).once()

          (mockEventRequestManager.createModificationWatchpointRequest _)
            .expects(stubField)
            .returning(stub[ModificationWatchpointRequest]).once()
          modificationWatchpointManager.createModificationWatchpointRequestWithId(
            requestId,
            className,
            fieldName
          )
        }

        val actual = modificationWatchpointManager.modificationWatchpointRequestList
        actual should contain theSameElementsAs (expected)
      }
    }

    describe("#createModificationWatchpointRequestWithId") {
      it("should create the modification watchpoint request using the provided id") {
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

        val mockModificationWatchpointRequest = mock[ModificationWatchpointRequest]
        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .returning(mockModificationWatchpointRequest).once()

        (mockModificationWatchpointRequest.setSuspendPolicy _)
          .expects(EventRequest.SUSPEND_EVENT_THREAD).once()
        (mockModificationWatchpointRequest.setEnabled _).expects(true).once()

        val actual = modificationWatchpointManager.createModificationWatchpointRequestWithId(
          expected.get,
          testClassName,
          testFieldName
        )
        actual should be(expected)
      }

      it("should create the modification watchpoint request and return Success(id)") {
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

        val mockModificationWatchpointRequest = mock[ModificationWatchpointRequest]
        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .returning(mockModificationWatchpointRequest).once()

        (mockModificationWatchpointRequest.setSuspendPolicy _)
          .expects(EventRequest.SUSPEND_EVENT_THREAD).once()
        (mockModificationWatchpointRequest.setEnabled _).expects(true).once()

        val actual = modificationWatchpointManager.createModificationWatchpointRequestWithId(
          expected.get,
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }

      it("should return the exception if failed to create the modification watchpoint request") {
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

        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .throwing(expected.failed.get).once()

        val actual = modificationWatchpointManager.createModificationWatchpointRequestWithId(
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

        val actual = modificationWatchpointManager.createModificationWatchpointRequestWithId(
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

        val actual = modificationWatchpointManager.createModificationWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }
    }

    describe("#hasModificationWatchpointRequestWithId") {
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

        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .returning(stub[ModificationWatchpointRequest]).once()

        modificationWatchpointManager.createModificationWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )

        val actual = modificationWatchpointManager.hasModificationWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }

      it("should return false if it does not exist") {
        val expected = false

        val actual = modificationWatchpointManager.hasModificationWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }
    }

    describe("#hasModificationWatchpointRequest") {
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

        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .returning(stub[ModificationWatchpointRequest]).once()

        modificationWatchpointManager.createModificationWatchpointRequest(
          testClassName,
          testFieldName
        )

        val actual = modificationWatchpointManager.hasModificationWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }

      it("should return false if it does not exist") {
        val expected = false
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val actual = modificationWatchpointManager.hasModificationWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }
    }

    describe("#getModificationWatchpointRequestWithId") {
      it("should return Some(ModificationWatchpointRequest) if found") {
        val expected = Some(stub[ModificationWatchpointRequest])
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .returning(expected.get).once()

        modificationWatchpointManager.createModificationWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )

        val actual = modificationWatchpointManager.getModificationWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }

      it("should return None if not found") {
        val expected = None

        val actual = modificationWatchpointManager.getModificationWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }
    }

    describe("#getModificationWatchpointRequest") {
      it("should return Some(Seq(ModificationWatchpointRequest)) if found") {
        val expected = Seq(stub[ModificationWatchpointRequest])
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .returning(expected.head).once()

        modificationWatchpointManager.createModificationWatchpointRequest(
          testClassName,
          testFieldName
        )

        val actual = modificationWatchpointManager.getModificationWatchpointRequest(
          testClassName,
          testFieldName
        ).get
        actual should contain theSameElementsAs (expected)
      }

      it("should return None if not found") {
        val expected = None
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val actual = modificationWatchpointManager.getModificationWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }
    }

    describe("#removeModificationWatchpointRequestWithId") {
      it("should return true if the modification watchpoint request was removed") {
        val expected = true
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"
        val stubRequest = stub[ModificationWatchpointRequest]

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .returning(stubRequest).once()

        modificationWatchpointManager.createModificationWatchpointRequestWithId(
          TestRequestId,
          testClassName,
          testFieldName
        )

        (mockEventRequestManager.deleteEventRequest _)
          .expects(stubRequest).once()

        val actual = modificationWatchpointManager.removeModificationWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }

      it("should return false if the modification watchpoint request was not removed") {
        val expected = false

        val actual = modificationWatchpointManager.removeModificationWatchpointRequestWithId(TestRequestId)
        actual should be (expected)
      }
    }

    describe("#removeModificationWatchpointRequest") {
      it("should return true if the modification watchpoint request was removed") {
        val expected = true
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"
        val stubRequest = stub[ModificationWatchpointRequest]

        val stubField = createFieldStub(testFieldName)
        val mockReferenceType = mock[ReferenceType]
        (mockClassManager.allClasses _).expects()
          .returning(Seq(mockReferenceType)).once()
        (mockReferenceType.name _).expects().returning(testClassName).once()
        (mockReferenceType.allFields _).expects()
          .returning(Seq(stubField).asJava).once()

        (mockEventRequestManager.createModificationWatchpointRequest _)
          .expects(stubField)
          .returning(stubRequest).once()

        modificationWatchpointManager.createModificationWatchpointRequest(
          testClassName,
          testFieldName
        )

        (mockEventRequestManager.deleteEventRequest _)
          .expects(stubRequest).once()

        val actual = modificationWatchpointManager.removeModificationWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }

      it("should return false if the modification watchpoint request was not removed") {
        val expected = false
        val testClassName = "full.class.name"
        val testFieldName = "fieldName"

        val actual = modificationWatchpointManager.removeModificationWatchpointRequest(
          testClassName,
          testFieldName
        )
        actual should be (expected)
      }
    }
  }
}
