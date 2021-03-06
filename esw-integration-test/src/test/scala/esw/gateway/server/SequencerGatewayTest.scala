package esw.gateway.server

import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error, Started}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.gateway.api.clients.ClientFactory
import esw.gateway.api.codecs.GatewayCodecs
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.{EventServer, Gateway}

class SequencerGatewayTest extends EswTestKit(Gateway, EventServer) with GatewayCodecs {
  private val subsystem     = ESW
  private val observingMode = "MoonNight" // TestScript2.kts

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnSequencerRef(subsystem, observingMode)
  }

  "SequencerApi" must {

    "handle submit, queryFinal commands | ESW-250" in {
      val clientFactory = new ClientFactory(gatewayPostClient, gatewayWsClient)

      val sequence    = Sequence(Setup(Prefix("esw.test"), CommandName("command-2"), Some(ObsId("obsId"))))
      val componentId = ComponentId(Prefix(s"$subsystem.$observingMode"), ComponentType.Sequencer)

      val sequencer = clientFactory.sequencer(componentId)

      //submit sequence
      val submitResponse = sequencer.submit(sequence).futureValue
      submitResponse shouldBe a[Started]

      //queryFinal
      sequencer.queryFinal(submitResponse.runId).futureValue should ===(Completed(submitResponse.runId))
    }

    "handle submit, queryFinal commands with error | ESW-250" in {
      val clientFactory = new ClientFactory(gatewayPostClient, gatewayWsClient)

      val sequence    = Sequence(Setup(Prefix("esw.test"), CommandName("fail-command"), Some(ObsId("obsId"))))
      val componentId = ComponentId(Prefix(s"$subsystem.$observingMode"), ComponentType.Sequencer)

      val sequencer = clientFactory.sequencer(componentId)

      //submit sequence
      val submitResponse = sequencer.submit(sequence).futureValue
      submitResponse shouldBe a[Started]

      //queryFinal
      sequencer.queryFinal(submitResponse.runId).futureValue should ===(
        Error(submitResponse.runId, "java.lang.RuntimeException: fail-command")
      )
    }
  }
}
