package esw.contract.data.gateway

import akka.Done
import csw.alarm.models.AlarmSeverity
import csw.contract.generator.ClassNameHelpers._
import csw.contract.generator._
import csw.logging.models.LogMetadata
import csw.params.commands.CommandResponse.{SubmitResponse, ValidateResponse}
import csw.params.events.Event
import csw.prefix.models.Subsystem
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.PostRequest.{
  ComponentCommand,
  GetEvent,
  GetLogMetadata,
  Log,
  PublishEvent,
  SequencerCommand,
  SetAlarmSeverity,
  SetLogLevel
}
import esw.gateway.api.protocol.WebsocketRequest.{Subscribe, SubscribeWithPattern}
import esw.gateway.api.protocol.{
  EmptyEventKeys,
  EventServerUnavailable,
  GatewayException,
  InvalidComponent,
  InvalidMaxFrequency,
  PostRequest,
  SetAlarmSeverityFailure,
  WebsocketRequest
}
import esw.ocs.api.protocol.OkOrUnhandledResponse
import io.bullet.borer.Encoder

object GatewayContract extends GatewayCodecs with GatewayData {
  val models: ModelSet = ModelSet(
    ModelType[Event](observeEvent, systemEvent),
    ModelType(AlarmSeverity),
    ModelType(prefix),
    ModelType(eventKey),
    ModelType(componentId),
    ModelType(logMetadata),
    ModelType(Subsystem),
    ModelType[GatewayException](
      invalidComponent,
      emptyEventKeys,
      eventServerUnavailable,
      invalidMaxFrequency,
      setAlarmSeverityFailure
    )
  )

  implicit def httpEnc[Sub <: PostRequest]: Encoder[Sub]           = SubTypeCodec.encoder(postRequestValue)
  implicit def websocketEnc[Sub <: WebsocketRequest]: Encoder[Sub] = SubTypeCodec.encoder(websocketRequestCodecValue)

  val httpRequests: ModelSet = ModelSet(
    ModelType(postComponentCommand),
    ModelType(postSequencerCommand),
    ModelType(publishEvent),
    ModelType(getEvent),
    ModelType(setAlarmSeverity),
    ModelType(log),
    ModelType(setLogLevel),
    ModelType(getLogMetadata)
  )

  val websocketRequests: ModelSet = ModelSet(
    ModelType(websocketComponentCommand),
    ModelType(websocketSequencerCommand),
    ModelType(subscribe),
    ModelType(subscribeWithPattern)
  )

  val httpEndpoints: List[Endpoint] = List(
    Endpoint(
      name[ComponentCommand],
      name[ValidateResponse],
      List(name[InvalidComponent]),
      Some(
        "Response type will depend on the command passed to componentCommand request. For all possible request and response type mappings refer to HTTP endpoint documentation of command service in CSW."
      )
    ),
    Endpoint(
      name[SequencerCommand],
      name[OkOrUnhandledResponse],
      List(name[InvalidComponent]),
      Some(
        "Response type will depend on the command passed to sequencerCommand request. For all possible request and response type mappings refer to HTTP endpoint documentation of sequencer service in ESW."
      )
    ),
    Endpoint(name[PublishEvent], name[Done], List(name[EventServerUnavailable])),
    Endpoint(name[GetEvent], arrayName[Event], List(name[EmptyEventKeys], name[EventServerUnavailable])),
    Endpoint(name[SetAlarmSeverity], name[Done], List(name[SetAlarmSeverityFailure])),
    Endpoint(name[Log], name[Done]),
    Endpoint(name[SetLogLevel], name[Unit]),
    Endpoint(name[GetLogMetadata], name[LogMetadata])
  )

  val webSocketEndpoints: List[Endpoint] = List(
    Endpoint(
      name[WebsocketRequest.ComponentCommand],
      name[SubmitResponse],
      description = Some(
        "Response type will depend on the command passed to componentCommand request. For all possible request and response type mappings refer to websocket endpoint documentation of command service in CSW."
      )
    ),
    Endpoint(
      name[WebsocketRequest.SequencerCommand],
      name[SubmitResponse],
      description = Some(
        "Response type will depend on the command passed to sequencerCommand request. For all possible request and response type mappings refer to websocket endpoint documentation of sequencer service in ESW."
      )
    ),
    Endpoint(name[Subscribe], name[Event], List(name[EmptyEventKeys], name[InvalidMaxFrequency])),
    Endpoint(name[SubscribeWithPattern], name[Event], List(name[InvalidMaxFrequency]))
  )

  val service: Service = Service(
    `http-contract` = Contract(httpEndpoints, httpRequests),
    `websocket-contract` = Contract(webSocketEndpoints, websocketRequests),
    models = models
  )
}
