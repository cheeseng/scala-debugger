package org.scaladebugger.api.profiles.pure.threads
import acyclic.file

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import org.scaladebugger.api.utils.JDITools
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Milliseconds, Seconds, Span}
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import org.scaladebugger.api.profiles.pure.PureDebugProfile
import org.scaladebugger.api.virtualmachines.DummyScalaVirtualMachine
import test.{TestUtilities, VirtualMachineFixtures}

class PureThreadStartProfileIntegrationSpec extends FunSpec with Matchers
  with ParallelTestExecution with VirtualMachineFixtures
  with TestUtilities with Eventually
{
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(test.Constants.EventuallyTimeout),
    interval = scaled(test.Constants.EventuallyInterval)
  )

  describe("PureThreadStartProfile") {
    it("should trigger when a thread starts") {
      val testClass = "org.scaladebugger.test.threads.ThreadStart"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      val threadStartCount = new AtomicInteger(0)

      val s = DummyScalaVirtualMachine.newInstance()

      // Mark that we want to receive thread start events
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeThreadStart()
        .map(_.thread().name())
        .filter(_.startsWith("test thread"))
        .foreach(_ => threadStartCount.incrementAndGet())

      // Start our Thread and listen for the start event
      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        // Eventually, we should receive a total of 10 thread starts
        logTimeTaken(eventually {
          threadStartCount.get() should be (10)
        })
      }
    }
  }
}
