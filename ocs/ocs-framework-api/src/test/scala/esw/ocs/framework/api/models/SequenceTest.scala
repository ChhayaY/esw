package esw.ocs.framework.api.models

import csw.params.commands.{CommandName, Observe, Setup}
import csw.params.core.models.Prefix
import esw.ocs.framework.api.BaseTestSuite

class SequenceTest extends BaseTestSuite {

  "empty" must {
    "create empty sequence" in {
      Sequence.empty.commands shouldBe Nil
    }
  }

  "apply" must {
    "create sequence from provided list of commands" in {
      val setup    = Setup(Prefix("test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("test1"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      sequence.commands shouldBe List(setup, observe)
    }
  }

  "add" must {
    "allow adding command to existing sequence" in {
      val setup    = Setup(Prefix("test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("test2"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      val newSetup       = Setup(Prefix("test3"), CommandName("setup-test"), None)
      val updateSequence = sequence.add(newSetup)
      updateSequence.commands shouldBe List(setup, observe, newSetup)
    }

    "allow adding list of commands to existing sequence" in {
      val setup    = Setup(Prefix("test1"), CommandName("setup-test"), None)
      val observe  = Observe(Prefix("test2"), CommandName("observe-test"), None)
      val sequence = Sequence(setup, observe)

      val newSetup   = Setup(Prefix("test3"), CommandName("setup-test"), None)
      val newObserve = Observe(Prefix("test4"), CommandName("setup-test"), None)

      val updateSequence = sequence.add(newSetup, newObserve)
      updateSequence.commands shouldBe List(setup, observe, newSetup, newObserve)
    }
  }
}