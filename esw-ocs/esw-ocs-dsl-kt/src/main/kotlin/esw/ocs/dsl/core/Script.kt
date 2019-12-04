package esw.ocs.dsl.core

import csw.params.commands.Observe
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.core.models.UTCTime
import esw.ocs.dsl.highlevel.CswHighLevelDsl
import esw.ocs.dsl.nullable
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.FsmScriptDsl
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptInitialisationFailedException
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlin.coroutines.CoroutineContext

sealed class BaseScript(val cswServices: CswServices, scope: CoroutineScope) : CswHighLevelDsl(cswServices), HandlerScope {
    internal open val scriptDsl: ScriptDsl by lazy { ScriptDsl(cswServices, strandEc) }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        warn("Exception thrown in script with a message: ${exception.message}, invoking exception handler", ex = exception)
        exception.printStackTrace()
        scriptDsl.executeExceptionHandlers(exception)
    }

    override val coroutineScope: CoroutineScope = scope + exceptionHandler

    fun onGoOnline(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onGoOnline { block.toCoroutineScope().toJava() }

    fun onGoOffline(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onGoOffline { block.toCoroutineScope().toJava() }

    fun onAbortSequence(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onAbortSequence { block.toCoroutineScope().toJava() }

    fun onShutdown(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onShutdown { block.toCoroutineScope().toJava() }

    fun onDiagnosticMode(block: suspend HandlerScope.(UTCTime, String) -> Unit) =
            scriptDsl.onDiagnosticMode { x: UTCTime, y: String ->
                coroutineScope.launch { block(this.toHandlerScope(), x, y) }.asCompletableFuture().thenAccept { }
            }

    fun onOperationsMode(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onOperationsMode { block.toCoroutineScope().toJava() }

    fun onStop(block: suspend HandlerScope.() -> Unit) =
            scriptDsl.onStop { block.toCoroutineScope().toJava() }

    internal fun CoroutineScope.toHandlerScope(): HandlerScope = object : HandlerScope by this@BaseScript {
        override val coroutineContext: CoroutineContext = this@toHandlerScope.coroutineContext
    }

    private fun (suspend HandlerScope.() -> Unit).toCoroutineScope(): suspend (CoroutineScope) -> Unit = { this(it.toHandlerScope()) }
}

open class Script(
        cswServices: CswServices,
        override val strandEc: StrandEc,
        scope: CoroutineScope
) : BaseScript(cswServices, scope), ScriptScope, CommandHandlerScope {

    //todo : revisit all the places implementing CoroutineContext
    override val coroutineContext: CoroutineContext = scope.coroutineContext // this won't be used anywhere

    override suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand? =
            scriptDsl.nextIf { predicate(it) }.await().nullable()

    override fun onSetup(name: String, block: suspend CommandHandlerScope.(Setup) -> Unit): CommandHandlerKt<Setup> {
        val handler = CommandHandlerKt(coroutineScope, block.toCoroutineScope())
        scriptDsl.onSetupCommand(name, handler)
        return handler
    }

    override fun onObserve(name: String, block: suspend CommandHandlerScope.(Observe) -> Unit): CommandHandlerKt<Observe> {
        val handler = CommandHandlerKt(coroutineScope, block.toCoroutineScope())
        scriptDsl.onObserveCommand(name, handler)
        return handler
    }

    override fun onException(block: suspend HandlerScope.(Throwable) -> Unit) =
            scriptDsl.onException {
                // "future" is used to swallow the exception coming from exception handlers
                coroutineScope.future { block(this.toHandlerScope(), it) }
                        .exceptionally { error("Exception thrown from Exception handler with a message : ${it.message}", ex = it) }
                        .thenAccept { }
            }

    override fun loadScripts(vararg reusableScriptResult: ReusableScriptResult) =
            reusableScriptResult.forEach {
                this.scriptDsl.merge(it(cswServices, strandEc, coroutineScope).scriptDsl)
            }

    override fun become(nextState: String, params: Params): Unit = throw RuntimeException("Become can not be called outside Fsm scripts")

    private fun <T> (suspend CommandHandlerScope.(T) -> Unit).toCoroutineScope(): suspend (CoroutineScope, T) -> Unit = { _scope, value ->
        val commandHandlerScope = object : CommandHandlerScope by this@Script {
            override val coroutineContext: CoroutineContext = _scope.coroutineContext
        }
        this.invoke(commandHandlerScope, value)
    }
}

class FsmScript(
        cswServices: CswServices,
        override val strandEc: StrandEc,
        private val scope: CoroutineScope
) : BaseScript(cswServices, scope), FsmScriptScope {
    internal val fsmScriptDsl: FsmScriptDsl by lazy { FsmScriptDsl(cswServices, strandEc) }

    override val coroutineContext: CoroutineContext = scope.coroutineContext

    override val scriptDsl: ScriptDsl by lazy { fsmScriptDsl }

    inner class FsmScriptStateDsl : Script(cswServices, strandEc, scope), FsmScriptStateScope {
        override val coroutineContext: CoroutineContext = this@FsmScript.scope.coroutineContext
        override fun become(nextState: String, params: Params) = this@FsmScript.become(nextState, params)
    }

    override fun state(name: String, block: suspend FsmScriptStateScope.(Params) -> Unit) {

        fun reusableScript(): FsmScriptStateDsl = FsmScriptStateDsl().apply {
            try {
                runBlocking { block(this@FsmScript.fsmScriptDsl.state.params()) }
            } catch (ex: Exception) {
                error("Failed to initialize state: $name", ex = ex)
                throw ScriptInitialisationFailedException(ex.message)
            }
        }

        fsmScriptDsl.add(name) { reusableScript().scriptDsl }
    }

    override fun become(nextState: String, params: Params) = fsmScriptDsl.become(nextState, params)
}
