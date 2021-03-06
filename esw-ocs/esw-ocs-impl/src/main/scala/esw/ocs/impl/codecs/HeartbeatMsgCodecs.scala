package esw.ocs.impl.codecs

import csw.command.client.cbor.MessageCodecs
import esw.ocs.impl.messages.HealthCheckMsg
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

trait HeartbeatMsgCodecs extends MessageCodecs {
  implicit lazy val heartbeatActorMessageCodec: Codec[HealthCheckMsg] = deriveAllCodecs
}
