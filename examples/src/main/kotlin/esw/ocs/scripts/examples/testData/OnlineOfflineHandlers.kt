package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript
import esw.ocs.dsl.highlevel.models.TCS
import kotlinx.coroutines.delay
import kotlin.time.seconds

// ESW-134: Reuse code by ability to import logic from one script into another
val OnlineOfflineHandlers = reusableScript {
    onGoOffline {
        val tcsSequencer = Sequencer(TCS, "moonnight", 10.seconds)
        tcsSequencer.goOffline()
        delay(1000)
    }

    onGoOnline {
        val tcsSequencer = Sequencer(TCS, "moonnight", 10.seconds)
        tcsSequencer.goOnline()
        delay(1000)
    }
}