package architecture

import cats.effect.IO
import cats.syntax.all._
import org.scalajs.dom

/** Error types for the Elm Architecture runtime */
sealed trait AppError extends Throwable {
  def message: String
  def userMessage: String
  override def getMessage: String = message
}

/** Runtime initialization errors */
case class InitializationError(message: String, cause: Option[Throwable] = None)
    extends AppError {
  val userMessage: String =
    "Application failed to start. Please refresh the page."
  cause.foreach(initCause)
}

/** Update function execution errors */
case class UpdateError(
    message: String,
    msg: Any,
    model: Any,
    cause: Option[Throwable] = None
) extends AppError {
  val userMessage: String =
    "An error occurred while processing your action. Please try again."
  cause.foreach(initCause)
}

/** View rendering errors */
case class RenderError(message: String, cause: Option[Throwable] = None)
    extends AppError {
  val userMessage: String = "Display error occurred. Please refresh the page."
  cause.foreach(initCause)
}

/** DOM operation errors */
case class DOMError(message: String, cause: Option[Throwable] = None)
    extends AppError {
  val userMessage: String =
    "Interface error occurred. Please try refreshing the page."
  cause.foreach(initCause)
}

/** Command execution errors */
case class CommandError(message: String, cause: Option[Throwable] = None)
    extends AppError {
  val userMessage: String = "Action failed to complete. Please try again."
  cause.foreach(initCause)
}

/** Subscription management errors */
case class SubscriptionError(message: String, cause: Option[Throwable] = None)
    extends AppError {
  val userMessage: String =
    "Background process error. Some features may not work properly."
  cause.foreach(initCause)
}

/** Storage operation errors */
case class StorageError(message: String, cause: Option[Throwable] = None)
    extends AppError {
  val userMessage: String =
    "Failed to save data. Your changes may not be preserved."
  cause.foreach(initCause)
}

/** Validation errors */
case class ValidationError(field: String, message: String, value: Any)
    extends AppError {
  val userMessage: String = s"Invalid $field: $message"
}

/** Network/external service errors */
case class NetworkError(message: String, cause: Option[Throwable] = None)
    extends AppError {
  val userMessage: String =
    "Network error occurred. Please check your connection and try again."
  cause.foreach(initCause)
}

/** Error recovery strategies */
object ErrorRecovery {

  /** Recover from errors with a fallback value */
  def withFallback[A](
      io: IO[A],
      fallback: A,
      logError: Boolean = true
  ): IO[A] = {
    io.handleErrorWith { error =>
      val logAction = if (logError) {
        IO.delay(
          dom.console.error(
            s"Error occurred, using fallback: ${error.getMessage}"
          )
        )
      } else IO.unit

      logAction *> IO.pure(fallback)
    }
  }

  /** Recover from errors with a fallback IO operation */
  def withFallbackIO[A](
      io: IO[A],
      fallback: IO[A],
      logError: Boolean = true
  ): IO[A] = {
    io.handleErrorWith { error =>
      val logAction = if (logError) {
        IO.delay(
          dom.console.error(
            s"Error occurred, trying fallback: ${error.getMessage}"
          )
        )
      } else IO.unit

      logAction *> fallback
    }
  }

  /** Retry an operation with exponential backoff */
  def withRetry[A](
      io: IO[A],
      maxRetries: Int = 3,
      baseDelay: scala.concurrent.duration.FiniteDuration =
        scala.concurrent.duration.FiniteDuration(100, "milliseconds")
  ): IO[A] = {
    def retry(attempt: Int): IO[A] = {
      io.handleErrorWith { error =>
        if (attempt >= maxRetries) {
          IO.raiseError(error)
        } else {
          val delay = baseDelay * math.pow(2, attempt).toLong
          IO.delay(
            dom.console.warn(
              s"Attempt ${attempt + 1} failed, retrying in ${delay.toMillis}ms: ${error.getMessage}"
            )
          ) *>
            IO.sleep(
              scala.concurrent.duration
                .FiniteDuration(delay.toMillis, "milliseconds")
            ) *>
            retry(attempt + 1)
        }
      }
    }
    retry(0)
  }

  /** Safe execution with error boundary */
  def safeExecute[A](
      io: IO[A],
      errorHandler: Throwable => IO[Unit] = defaultErrorHandler
  ): IO[Option[A]] = {
    io.map(Some(_)).handleErrorWith { error =>
      errorHandler(error) *> IO.pure(None)
    }
  }

  /** Default error handler that logs to console */
  def defaultErrorHandler(error: Throwable): IO[Unit] = {
    IO.delay {
      error match {
        case appError: AppError =>
          dom.console.error(s"Application Error: ${appError.message}")
        // In a real app, you might show a user notification here
        case _ =>
          dom.console.error(s"Unexpected Error: ${error.getMessage}")
      }
    }
  }

  /** Validate and execute with proper error handling */
  def validateAndExecute[A, B](
      value: A,
      validator: A => Either[ValidationError, B],
      action: B => IO[Unit]
  ): IO[Unit] = {
    validator(value) match {
      case Right(validValue) =>
        action(validValue).handleErrorWith { error =>
          IO.delay(
            dom.console.error(
              s"Action failed after validation: ${error.getMessage}"
            )
          )
        }
      case Left(validationError) =>
        IO.delay(
          dom.console.warn(s"Validation failed: ${validationError.userMessage}")
        )
    }
  }
}

/** Error boundary for update functions */
object UpdateErrorBoundary {

  /** Wrap an update function with error handling */
  def wrap[Model, Msg](
      updateFn: (Msg, Model) => Update[Model, Msg]
  ): (Msg, Model) => Update[Model, Msg] = { (msg, model) =>
    try {
      updateFn(msg, model)
    } catch {
      case error: Throwable =>
        dom.console.error(s"Update function error: ${error.getMessage}")
        // Return the unchanged model with no command
        Update(model, Cmd.none)
    }
  }

  /** Wrap an update function with error handling and recovery */
  def wrapWithRecovery[Model, Msg](
      updateFn: (Msg, Model) => Update[Model, Msg],
      errorRecovery: (Throwable, Msg, Model) => Update[Model, Msg]
  ): (Msg, Model) => Update[Model, Msg] = { (msg, model) =>
    try {
      updateFn(msg, model)
    } catch {
      case error: Throwable =>
        dom.console.error(
          s"Update function error, attempting recovery: ${error.getMessage}"
        )
        try {
          errorRecovery(error, msg, model)
        } catch {
          case recoveryError: Throwable =>
            dom.console.error(
              s"Error recovery failed: ${recoveryError.getMessage}"
            )
            Update(model, Cmd.none)
        }
    }
  }
}

/** Error boundary for view functions */
object ViewErrorBoundary {

  /** Wrap a view function with error handling */
  def wrap[Model](
      viewFn: Model => vdom.VNode,
      fallbackView: (Model, Throwable) => vdom.VNode = defaultFallbackView
  ): Model => vdom.VNode = { model =>
    try {
      viewFn(model)
    } catch {
      case error: Throwable =>
        dom.console.error(s"View function error: ${error.getMessage}")
        fallbackView(model, error)
    }
  }

  /** Default fallback view for errors */
  def defaultFallbackView[Model](model: Model, error: Throwable): vdom.VNode = {
    import vdom.Html._
    div("class" -> "error-boundary")(
      div("class" -> "error-title")(text("Something went wrong")),
      div("class" -> "error-message")(
        text("Please refresh the page to continue.")
      ),
      div("class" -> "error-details")(
        div("class" -> "error-summary")(text("Error details")),
        div("class" -> "error-trace")(text(error.getMessage))
      )
    )
  }
}

/** Graceful degradation utilities */
object GracefulDegradation {

  /** Check if localStorage is available */
  def isLocalStorageAvailable: IO[Boolean] = {
    IO.delay {
      try {
        val testKey = "test-storage-availability"
        val testValue = "test"
        dom.window.localStorage.setItem(testKey, testValue)
        val retrieved = dom.window.localStorage.getItem(testKey)
        dom.window.localStorage.removeItem(testKey)
        retrieved == testValue
      } catch {
        case _: Throwable => false
      }
    }
  }

  /** Check if a DOM element exists */
  def elementExists(id: String): IO[Boolean] = {
    IO.delay {
      Option(dom.document.getElementById(id)).isDefined
    }
  }

  /** Safe DOM element access */
  def safeGetElement(id: String): IO[Option[dom.Element]] = {
    IO.delay {
      Option(dom.document.getElementById(id))
    }.handleErrorWith { _ =>
      IO.pure(None)
    }
  }

  /** Check browser capabilities */
  def checkBrowserCapabilities: IO[Map[String, Boolean]] = {
    IO.delay {
      Map(
        "localStorage" -> (try { dom.window.localStorage != null }
        catch { case _: Throwable => false }),
        "sessionStorage" -> (try { dom.window.sessionStorage != null }
        catch { case _: Throwable => false }),
        "console" -> (try { dom.console != null }
        catch { case _: Throwable => false }),
        "JSON" -> (try { scala.scalajs.js.JSON != null }
        catch { case _: Throwable => false })
      )
    }
  }
}
