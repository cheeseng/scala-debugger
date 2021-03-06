package org.scaladebugger.api.profiles.pure.info

import com.sun.jdi.ThreadReference
import org.scaladebugger.api.lowlevel.events.misc.NoResume
import org.scaladebugger.api.profiles.pure.PureDebugProfile
import org.scaladebugger.api.utils.JDITools
import org.scaladebugger.api.virtualmachines.DummyScalaVirtualMachine
import org.scalatest.concurrent.Eventually
import org.scalatest.{FunSpec, Matchers, ParallelTestExecution}
import test.{TestUtilities, VirtualMachineFixtures}

class PureObjectInfoProfileIntegrationSpec extends FunSpec with Matchers
  with ParallelTestExecution with VirtualMachineFixtures
  with TestUtilities with Eventually
{
  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(test.Constants.EventuallyTimeout),
    interval = scaled(test.Constants.EventuallyInterval)
  )

  describe("PureObjectInfoProfile") {
    it("should be able to invoke a method on the object") {
      val testClass = "org.scaladebugger.test.info.Methods"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 22, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val result = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame.withUnsafeThisObject
            .unsafeInvoke("publicMethod", Seq(3, "test")).asUnsafeLocalValue

          result should be ("3test")
        })
      }
    }

    it("should be able to retrieve a specific field from the object") {
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
          val fieldName = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame.withUnsafeThisObject
            .unsafeField("z1").name

          fieldName should be ("z1")
        })
      }
    }

    it("should be able to get a list of fields for the object") {
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
          val fieldNames = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame.withUnsafeThisObject
            .unsafeFields.map(_.name)

          fieldNames should contain theSameElementsAs Seq(
            "MODULE$", "z1", "z2", "z3"
          )
        })
      }
    }

    it("should be able to retrieve a specific method from the object") {
      val testClass = "org.scaladebugger.test.info.Methods"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 22, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val methodName = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame.withUnsafeThisObject
            .unsafeMethod("publicMethod").name

          methodName should be ("publicMethod")
        })
      }
    }

    it("should be able to get a list of methods for the object") {
      val testClass = "org.scaladebugger.test.info.Methods"
      val testFile = JDITools.scalaClassStringToFileString(testClass)

      @volatile var t: Option[ThreadReference] = None
      val s = DummyScalaVirtualMachine.newInstance()

      // NOTE: Do not resume so we can check the variables at the stack frame
      s.withProfile(PureDebugProfile.Name)
        .onUnsafeBreakpoint(testFile, 22, NoResume)
        .foreach(e => t = Some(e.thread()))

      withVirtualMachine(testClass, pendingScalaVirtualMachines = Seq(s)) { (s) =>
        logTimeTaken(eventually {
          val methodNames = s.withProfile(PureDebugProfile.Name)
            .forUnsafeThread(t.get).withUnsafeTopFrame.withUnsafeThisObject
            .unsafeMethods.map(_.name)

          methodNames should contain theSameElementsAs Seq(
            // Defined methods
            "main",
            "innerMethod$1", // Nested method has different Java signature
            "publicMethod",
            "privateMethod",
            "protectedMethod",
            "zeroArgMethod",
            "functionMethod", // Scala provides a method for the function
                              // object since it would be treated as a field

            // Inherited methods
            "<clinit>",
            "<init>",
            "registerNatives",
            "getClass",
            "hashCode",
            "equals",
            "clone",
            "toString",
            "notify",
            "notifyAll",
            "wait", // Overloaded method
            "wait",
            "wait",
            "finalize"
          )
        })
      }
    }
  }
}
