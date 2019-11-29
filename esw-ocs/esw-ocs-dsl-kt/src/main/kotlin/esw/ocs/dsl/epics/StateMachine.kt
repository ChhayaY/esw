package esw.ocs.dsl.epics

import esw.ocs.dsl.params.Params
import kotlinx.coroutines.*
import kotlin.time.Duration

@DslMarker
annotation class FSMDslMarker

// this interface is exposed to outside world
interface StateMachine : Refreshable {
    suspend fun start()
    suspend fun await()
}

// this interface is exposed at top-level of FSM
@FSMDslMarker
interface FSMTopLevel {
    fun state(name: String, block: suspend FSMState.(params: Params) -> Unit)
}

// this interface is exposed in side each state of FSM
@FSMDslMarker
interface FSMState {
    suspend fun become(state: String, params: Params = Params(setOf()))
    fun completeFSM()
    suspend fun on(condition: Boolean = true, body: suspend () -> Unit)
    suspend fun after(duration: Duration, body: suspend () -> Unit)
    suspend fun entry(body: suspend () -> Unit)
}

// Don't remove name parameter, it will be used while logging.
class StateMachineImpl(val name: String, private val initialState: String, val coroutineScope: CoroutineScope) : StateMachine, FSMTopLevel, FSMState {
    // fixme: Try to remove optional behavior of both variables
    private var currentState: String? = null
    private var previousState: String? = null
    private var params: Params = Params(setOf())

    //fixme : do we need to pass as receiver coroutine scope to state lambda
    private val states = mutableMapOf<String, suspend FSMState.(params: Params) -> Unit>()

    //this is done to make new job child of the coroutine scope's job.
    private val fsmJob: CompletableJob = Job(coroutineScope.coroutineContext[Job])

    override fun state(name: String, block: suspend FSMState.(params: Params) -> Unit) {
        states += name.toUpperCase() to block
    }

    override suspend fun become(state: String, params: Params) {
        if (states.keys.any { it.equals(state, true) }) {
            previousState = currentState
            currentState = state
            this.params = params
            coroutineScope.launch(fsmJob) {
                states[currentState?.toUpperCase()]?.invoke(this@StateMachineImpl, params)
            }.join()
        } else throw InvalidStateException(state)
    }

    override suspend fun start() = become(initialState)

    override suspend fun await() = fsmJob.join()

    override fun completeFSM() = fsmJob.cancel()

    // fixme: remove !!
    override suspend fun refresh() = become(currentState!!, params)

    override suspend fun on(condition: Boolean, body: suspend () -> Unit) {
        if (condition) {
            body()
        }
    }

    override suspend fun after(duration: Duration, body: suspend () -> Unit) {
        delay(duration.toLongMilliseconds())
        body()
    }

    override suspend fun entry(body: suspend () -> Unit) {
        if (!currentState.equals(previousState, true)) {
            body()
        }
    }
}
