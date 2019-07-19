package esw.ocs.core

import java.net.URI

import akka.Done
import akka.actor.testkit.typed.scaladsl.ActorTestKitBase
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{AkkaLocation, ComponentId, ComponentType}
import csw.params.core.models.Prefix
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.messages.SequenceComponentMsg
import esw.ocs.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SequenceComponentClientTest extends ActorTestKitBase with BaseTestSuite {

  implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")

  override protected def afterAll(): Unit = {
    actorSystem.terminate
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }

  private val location =
    AkkaLocation(AkkaConnection(ComponentId("test", ComponentType.Sequencer)), Prefix("test"), new URI("uri"))
  private val loadScriptResponse = Right(location)
  private val getStatusResponse  = Some(location)

  private val mockedBehavior: Behaviors.Receive[SequenceComponentMsg] = Behaviors.receiveMessage[SequenceComponentMsg] { msg =>
    msg match {
      case LoadScript(_, _, replyTo) => replyTo ! loadScriptResponse
      case GetStatus(replyTo)        => replyTo ! getStatusResponse
      case UnloadScript(replyTo)     => replyTo ! Done
      case _                         =>
    }
    Behaviors.same
  }

  private val sequenceComponent = spawn(mockedBehavior)

  private val sequenceComponentClient = new SequenceComponentClient(sequenceComponent)

  "LoadScript | ESW-103" in {
    sequenceComponentClient.loadScript("sequencerId", "observingMode").futureValue should ===(loadScriptResponse)
  }

  "GetStatus | ESW-103" in {
    sequenceComponentClient.getStatus.futureValue should ===(getStatusResponse)
  }

  "UnloadScript | ESW-103" in {
    sequenceComponentClient.unloadScript().futureValue should ===(Done)
  }
}