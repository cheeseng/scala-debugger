package org.scaladebugger.api.lowlevel.monitors
import acyclic.file

import org.scalamock.scalatest.MockFactory
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import org.scaladebugger.api.lowlevel.requests.JDIRequestArgument
import test.TestMonitorContendedEnteredManager

import scala.util.Success

class MonitorContendedEnteredManagerSpec extends FunSpec with Matchers
  with ParallelTestExecution with MockFactory
{
  private val TestRequestId = java.util.UUID.randomUUID().toString
  private val mockMonitorContendedEnteredManager = mock[MonitorContendedEnteredManager]
  private val testMonitorContendedEnteredManager = new TestMonitorContendedEnteredManager(
    mockMonitorContendedEnteredManager
  ) {
    override protected def newRequestId(): String = TestRequestId
  }

  describe("MonitorContendedEnteredManager") {
    describe("#createMonitorContendedEnteredRequest") {
      it("should invoke createMonitorContendedEnteredRequestWithId") {
        val expected = Success(TestRequestId)
        val testExtraArguments = Seq(stub[JDIRequestArgument])

        (mockMonitorContendedEnteredManager.createMonitorContendedEnteredRequestWithId _)
          .expects(TestRequestId, testExtraArguments)
          .returning(expected).once()

        val actual = testMonitorContendedEnteredManager.createMonitorContendedEnteredRequest(
          testExtraArguments: _*
        )

        actual should be (expected)
      }
    }

    describe("#createMonitorContendedEnteredRequestFromInfo") {
      it("should invoke createMonitorContendedEnteredRequestWithId") {
        val expected = Success(TestRequestId)
        val testExtraArguments = Seq(stub[JDIRequestArgument])

        (mockMonitorContendedEnteredManager.createMonitorContendedEnteredRequestWithId _)
          .expects(TestRequestId, testExtraArguments)
          .returning(expected).once()

        val info = MonitorContendedEnteredRequestInfo(
          TestRequestId,
          testExtraArguments
        )
        val actual = testMonitorContendedEnteredManager.createMonitorContendedEnteredRequestFromInfo(info)

        actual should be(expected)
      }
    }
  }
}
