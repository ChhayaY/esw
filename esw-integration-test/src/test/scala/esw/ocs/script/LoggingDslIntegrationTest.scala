package esw.ocs.script

import akka.actor.typed.ActorRef
import csw.command.client.messages.sequencer.SequencerMsg
import csw.logging.client.internal.JsonExtensions.RichJsObject
import csw.logging.client.internal.LoggingSystem
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.gateway.server.TestAppender
import esw.ocs.api.SequencerCommandApi
import esw.ocs.impl.SequencerCommandImpl
import esw.ocs.testkit.EswTestKit
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable

class LoggingDslIntegrationTest extends EswTestKit {

  private var ocsRef: ActorRef[SequencerMsg]  = _
  private var ocsCommand: SequencerCommandApi = _

  private val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  private val testAppender                        = new TestAppender(x => logBuffer += Json.parse(x.toString).as[JsObject])

  var loggingSystem: LoggingSystem = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    ocsRef = spawnSequencerRef("ocs", "moonnight", None)
    ocsCommand = new SequencerCommandImpl(ocsRef)
    loggingSystem = LoggingSystemFactory.start("LoggingDslIntegrationTest", "", "", system)
    loggingSystem.setAppenders(List(testAppender))
  }

  "Script" must {
    "be able to log message | ESW-127" in {
      val command  = Setup(Prefix("TCS.test"), CommandName("log-command"), None)
      val sequence = Sequence(command)

      ocsCommand.submitAndWait(sequence).futureValue shouldBe a[Completed]
      Thread.sleep(500)

      val log: JsObject = logBuffer.head
      log.getString("@componentName") shouldBe "ocs@moonnight"
      log.getString("@severity") shouldBe "FATAL"
      log.getString("prefix") shouldBe "Prefix(esw.ocs.prefix5)"
      log.getString("class") shouldBe "esw.ocs.scripts.examples.testData.TestScript2"
      log.getString("message") shouldBe "log-message"
    }
  }
}