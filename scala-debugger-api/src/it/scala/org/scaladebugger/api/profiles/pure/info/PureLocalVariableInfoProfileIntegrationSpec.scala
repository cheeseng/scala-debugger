package org.scaladebugger.api.profiles.pure.info
import acyclic.file

import com.sun.jdi.{InvalidTypeException, ThreadReference}
import org.scaladebugger.api.lowlevel.events.misc.NoResume
import org.scaladebugger.api.profiles.pure.PureDebugProfile
import org.scaladebugger.api.utils.JDITools
import org.scaladebugger.api.virtualmachines.DummyScalaVirtualMachine
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import test.{TestUtilities, VirtualMachineFixtures}

class PureLocalVariableInfoProfileIntegrationSpec extends FunSpec with Matchers
  with ParallelTestExecution with VirtualMachineFixtures
  with TestUtilities with Eventually
{
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(test.Constants.EventuallyTimeout),
    interval = scaled(test.Constants.EventuallyInterval)
  )

  describe("PureLocalVariableInfoProfile") {
    it("should be able to get the name of the variable") {
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
          val variable = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("h")

          variable.name should be ("h")
        })
      }
    }

    it("should indicate that the variable is not a field") {
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
          val variable = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("h")

          variable.isField should be (false)
        })
      }
    }

    it("should indicate that the variable is a local variable") {
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
          val variable = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("h")

          variable.isLocal should be (true)
        })
      }
    }

    it("should indicate whether or not the variable is an argument") {
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
          s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("h")
            .isArgument should be (false)

          s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("args")
            .isArgument should be (true)
        })
      }
    }

    it("should be able to change the value of a non-final variable") {
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
          val variable = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("g")

          variable.setValue(888.0).get should be (888.0)
        })
      }
    }

    it("should be unable to change the value of a string local variable") {
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
          val field = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame
            .forUnsafeVariable("h")

          intercept[InvalidTypeException] {
            field.setValue("some value").get
          }
        })
      }
    }
  }
}
