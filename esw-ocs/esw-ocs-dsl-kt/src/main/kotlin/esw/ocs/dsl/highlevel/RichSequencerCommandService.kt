package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.SequencerCommandServiceFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.api.protocol.*
import esw.ocs.dsl.Timeouts
import esw.ocs.dsl.jdk.toJava
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import kotlinx.coroutines.future.await

class RichSequencerCommandService(
        private val sequencerId: String,
        private val observingMode: String,
        private val sequencerAdminFactory: SequencerAdminFactoryApi,
        private val locationServiceUtil: LocationServiceUtil,
        private val actorSystem: ActorSystem<*>
) {

    private suspend fun sequencerCommandService(): SequencerCommandService {
        val sequencerLocation =
                locationServiceUtil.resolveSequencer(sequencerId, observingMode, Timeouts.DefaultTimeout()).toJava().await()
        return SequencerCommandServiceFactory.make(sequencerLocation, actorSystem)
    }

    private suspend fun sequencerAdmin() =
            sequencerAdminFactory.make(sequencerId, observingMode).toJava().await()

    suspend fun submitAndWait(sequence: Sequence): SubmitResponse? =
            sequencerCommandService().submitAndWait(sequence).toJava().await()

    suspend fun goOnline(): GoOnlineResponse? =
            sequencerAdmin().goOnline().toJava().await()

    suspend fun goOffline(): GoOfflineResponse? =
            sequencerAdmin().goOffline().toJava().await()

    suspend fun diagnosticMode(startTime: UTCTime, hint: String): DiagnosticModeResponse? =
            sequencerAdmin().diagnosticMode(startTime, hint).toJava().await()

    suspend fun operationsMode(): OperationsModeResponse? =
            sequencerAdmin().operationsMode().toJava().await()

    suspend fun abortSequence(): OkOrUnhandledResponse? =
            sequencerAdmin().abortSequence().toJava().await()

    suspend fun stop(): OkOrUnhandledResponse? =
            sequencerAdmin().stop().toJava().await()

}
