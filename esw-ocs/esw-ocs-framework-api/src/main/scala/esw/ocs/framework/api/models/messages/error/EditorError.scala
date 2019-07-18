package esw.ocs.framework.api.models.messages.error

import csw.params.core.models.Id
import esw.ocs.framework.api.models.StepStatus
import esw.ocs.framework.api.models.serializer.OcsFrameworkAkkaSerializable

sealed trait EditorError extends OcsFrameworkAkkaSerializable

case class SequencerShutdownError(msg: String) extends EditorError
case class SequencerAbortError(msg: String)    extends EditorError

sealed trait StepListError extends Product with Serializable with EditorError

object StepListError {

  case class NotSupported(stepStatus: StepStatus)
      extends InsertError
      with ReplaceError
      with DeleteError
      with AddBreakpointError
      with PauseError

  case object NotAllowedOnFinishedSeq
      extends AddBreakpointError
      with PauseError
      with UpdateError
      with AddError
      with ResumeError
      with ResetError
      with ReplaceError
      with PrependError
      with DeleteError
      with InsertError
      with RemoveBreakpointError

  final case class IdDoesNotExist(id: Id)
      extends ReplaceError
      with InsertError
      with UpdateError
      with DeleteError
      with AddBreakpointError
      with RemoveBreakpointError

  sealed trait AddBreakpointError extends StepListError
  sealed trait PauseError         extends StepListError
  case object PauseFailed         extends PauseError

  case class AddingBreakpointNotSupported(status: StepStatus) extends AddBreakpointError with PauseError

  sealed trait ResumeError extends StepListError

  sealed trait UpdateError                                        extends StepListError
  case class UpdateNotSupported(from: StepStatus, to: StepStatus) extends UpdateError

  sealed trait AddError extends StepListError
  case object AddFailed extends AddError

  sealed trait PrependError extends StepListError

  sealed trait ResetError extends StepListError

  sealed trait InsertError extends StepListError

  sealed trait ReplaceError extends StepListError

  sealed trait DeleteError extends StepListError

  sealed trait RemoveBreakpointError extends StepListError

}