package org.scaladebugger.api.profiles.pure.info

import com.sun.jdi.ThreadReference
import org.scaladebugger.api.lowlevel.events.misc.NoResume
import org.scaladebugger.api.profiles.pure.PureDebugProfile
import org.scaladebugger.api.utils.JDITools
import org.scaladebugger.api.virtualmachines.DummyScalaVirtualMachine
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import test.{TestUtilities, VirtualMachineFixtures}

class PureArrayInfoProfileIntegrationSpec extends FunSpec with Matchers
  with ParallelTestExecution with VirtualMachineFixtures
  with TestUtilities with Eventually
{
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(test.Constants.EventuallyTimeout),
    interval = scaled(test.Constants.EventuallyInterval)
  )

  describe("PureArrayInfoProfile") {
    it("should be able to return the length of the array") {
      val testClass = "org.scaladebugger.test.info.Variables"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 32, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val array = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("i").toUnsafeValue.asUnsafeArray

          array.length should be (3)
        })
      }
    }

    it("should be able to return an element at a specific position") {
      val testClass = "org.scaladebugger.test.info.Variables"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 32, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val array = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("i").toUnsafeValue.asUnsafeArray

          array(1).asUnsafeLocalValue should be (2)
        })
      }
    }

    it("should be able to return a range of elements") {
      val testClass = "org.scaladebugger.test.info.Variables"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 32, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val array = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("i").toUnsafeValue.asUnsafeArray

          array.getUnsafeValues(1, 2).map(_.asUnsafeLocalValue) should be (Seq(2, 3))
        })
      }
    }

    it("should be able to return all elements") {
      val testClass = "org.scaladebugger.test.info.Variables"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 32, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val array = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("i").toUnsafeValue.asUnsafeArray

          array.getUnsafeValues.map(_.asUnsafeLocalValue) should be (Seq(1, 2, 3))
        })
      }
    }

    it("should be able to set a value at a specific position") {
      val testClass = "org.scaladebugger.test.info.Variables"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 32, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val array = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("i").toUnsafeValue.asUnsafeArray

          array(1) = 999
          array(1).asUnsafeLocalValue should be (999)
        })
      }
    }

    it("should be able to set a range of values") {
      val testClass = "org.scaladebugger.test.info.Variables"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 32, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val array = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("i").toUnsafeValue.asUnsafeArray

          // Set element at position 1 to source element 2 (12)
          array.setUnsafeValues(1, Seq(10, 11, 12), 2, 1) should be (Seq(12))
        })
      }
    }

    it("should be able to set all values") {
      val testClass = "org.scaladebugger.test.info.Variables"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 32, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val array = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("i").toUnsafeValue.asUnsafeArray

          array.setUnsafeValues(Seq(10, 11, 12)) should be (Seq(10, 11, 12))
        })
      }
    }
  }
}
