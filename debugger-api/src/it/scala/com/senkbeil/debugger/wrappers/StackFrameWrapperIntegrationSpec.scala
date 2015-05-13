package com.senkbeil.debugger.wrappers

import com.senkbeil.debugger.events.EventType.BreakpointEventType
import com.sun.jdi.Value
import com.sun.jdi.event.BreakpointEvent
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import test.{TestUtilities, VirtualMachineFixtures}

import scala.util.Try

class StackFrameWrapperIntegrationSpec extends FunSpec with Matchers
  with ParallelTestExecution with VirtualMachineFixtures
  with TestUtilities with Eventually
{
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(5, Seconds)),
    interval = scaled(Span(5, Milliseconds))
  )

  describe("StackFrameWrapper") {
    it("should be able to set breakpoints within while loops") {
      val testClass = "com.senkbeil.test.misc.Variables"
      val testFile = scalaClassStringToFileString(testClass)
      val lastLine = 26

      withVirtualMachine(testClass, suspend = false) { (v, s) =>
        // Add a breakpoint after all of our variables
        s.breakpointManager.setLineBreakpoint(testFile, lastLine)

        @volatile var variableMap: Option[Map[String, Any]] = None
        s.eventManager.addResumingEventHandler(BreakpointEventType, e => {
          val breakpointEvent = e.asInstanceOf[BreakpointEvent]

          import com.senkbeil.debugger.wrappers._
          val threadReference = breakpointEvent.thread()
          val currentFrame = threadReference.frame(0)

          // Retrieve the local variables at the end of the program
          variableMap = Some(currentFrame.localVisibleVariableMap().map(v =>
            (v._1.name(), v._2.value())
          ).map(v => {
            // Process any arrays before the VM is closed
            val isArray =
              Try(v._2.asInstanceOf[java.util.List[Value]]).isSuccess
            (v._1, if (isArray) v._2.toString else v._2)
          }))
        })

        eventually {
          assert(variableMap.nonEmpty, "Breakpoint not hit yet!")
          val vMap = variableMap.get

          vMap("a").asInstanceOf[Boolean] should be (true)
          vMap("b") should be ('c')
          vMap("c") should be (3.asInstanceOf[Short])
          vMap("d") should be (4)
          vMap("e") should be (5L)
          vMap("f") should be (1.0f)
          vMap("g") should be (2.0)
          vMap("h") should be("test")

          // Java-based array of primitives
          val iString = vMap("i").asInstanceOf[String]
          iString should be ("[1, 2, 3]")

          // Scala-based list of primitives
          val jString = vMap("j").asInstanceOf[String]
          jString should include ("scala.collection.immutable.$colon$colon")

          // Scala-based list of objects
          val kString = vMap("k").asInstanceOf[String]
          kString should include ("com.senkbeil.test.misc.Variables$One")
          kString should include ("java.lang.Integer")
          kString should include ("java.lang.Boolean")
        }
      }
    }
  }
}