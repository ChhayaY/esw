package esw.ocs.framework.internal

import com.typesafe.config.{Config, ConfigException}
import csw.params.core.models.Prefix
import esw.ocs.framework.exceptions.ScriptLoadingException.ScriptConfigurationMissingException

private[framework] final case class SequencerConfig(sequencerName: String, prefix: Prefix, scriptClass: String)

object SequencerConfig {
  def from(config: Config, sequencerId: String, observingMode: String): SequencerConfig = {
    val scriptConfig =
      try {
        config.getConfig(s"scripts.$sequencerId.$observingMode")
      } catch {
        case _: ConfigException.Missing => throw new ScriptConfigurationMissingException(sequencerId, observingMode)
      }

    val name        = s"$sequencerId@$observingMode"
    val prefix      = scriptConfig.getString("prefix")
    val scriptClass = scriptConfig.getString("scriptClass")

    SequencerConfig(name, Prefix(prefix), scriptClass)
  }
}