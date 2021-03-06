package esw.ocs.dsl

import csw.params.commands.CommandResponse.*
import csw.params.commands.Result
import esw.ocs.dsl.highlevel.models.CommandError
import esw.ocs.dsl.highlevel.models.OtherError
import esw.ocs.dsl.highlevel.models.ScriptError
import java.util.*

// =========== SubmitResponse ===========
val SubmitResponse.isStarted: Boolean get() = this is Started
val SubmitResponse.isCompleted: Boolean get() = this is Completed
val SubmitResponse.isFailed: Boolean get() = isNegative(this)

suspend fun SubmitResponse.onStarted(block: suspend (Started) -> Unit): SubmitResponse {
    if (this is Started) block(this)
    return this
}

suspend fun SubmitResponse.onCompleted(block: suspend (Completed) -> Unit): SubmitResponse {
    if (this is Completed) block(this)
    return this
}

suspend fun SubmitResponse.onFailed(block: suspend (SubmitResponse) -> Unit): SubmitResponse {
    if (this.isFailed) block(this)
    return this
}

fun SubmitResponse.onFailedTerminate(): SubmitResponse {
    if (this.isFailed) throw CommandError(this)
    return this
}

// =========== SubmitResponse - unsafe extensions ===========
val SubmitResponse.completed: Completed
    get() = if (this is Completed) this else throw CommandError(this)

val SubmitResponse.result: Result
    get() = if (this is Completed) this.result() else throw CommandError(this)

// ==========================================================

fun <T> Optional<T>.nullable(): T? = orElse(null)

internal fun Throwable.toScriptError(): ScriptError = when (this) {
    is CommandError -> this
    else -> OtherError(this.message ?: "Unknown error", this)
}
