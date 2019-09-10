package esw.ocs.impl.dsl

import scala.annotation.compileTimeOnly
import scala.async.internal.ScalaConcurrentAsync
import scala.concurrent.{ExecutionContext, Future}
import scala.language.experimental.macros
// $COVERAGE-OFF$

object Async {
  def async[T](body: => T)(implicit execContext: ExecutionContext): Future[T] =
    macro ScalaConcurrentAsync.asyncImpl[T]

  @compileTimeOnly("`await` must be enclosed in an `spawn` block")
  def await[T](awaitable: Future[T]): T = ???
}

// $COVERAGE-ON$