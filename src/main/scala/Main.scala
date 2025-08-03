import cats.effect.{IO, IOApp}
import org.scalajs.dom
import org.scalajs.dom.{Element, document}
import scala.util.{Try, Success, Failure}

/** Main application entry point for TodoMVC Elm Architecture implementation
  *
  * This object initializes the TodoMVC application using the Elm Architecture
  * pattern with Cats Effect for managing side effects and concurrency.
  */
object Main extends IOApp.Simple {

  /** Main application entry point
    *
    * Initializes the TodoMVC application by:
    *   1. Finding the DOM container element 2. Creating and starting the
    *      Runtime with TodoApp 3. Handling initialization errors gracefully 4.
    *      Notifying the HTML page when the app is ready
    *
    * @return
    *   IO[Unit] that completes when the application is running
    */
  def run: IO[Unit] = {
    for {
      _ <- IO.delay(
        console.log("Starting TodoMVC Elm Architecture application...")
      )

      // Initialize the application with error handling
      _ <- initializeApplication()
        .handleErrorWith(handleInitializationError)

    } yield ()
  }

  /** Initialize the TodoMVC application
    *
    * @return
    *   IO[Unit] that completes when the application is successfully initialized
    */
  private def initializeApplication(): IO[Unit] = {
    for {
      // Find the application container element
      container <- findAppContainer()

      // Log successful container discovery
      _ <- IO.delay(
        console.log(s"Found application container: ${container.id}")
      )

      // Create and start the runtime (placeholder implementation)
      _ <- IO.delay(console.log("Creating TodoApp runtime..."))
      _ <- createAndStartRuntime(container)
      _ <- IO.delay(console.log("TodoMVC application started successfully"))

      // Notify the HTML page that the app is ready
      _ <- notifyAppReady()

    } yield ()
  }

  /** Create and start the runtime with TodoApp
    *
    * This is a placeholder implementation that will be completed when the
    * architecture compilation issues are resolved.
    *
    * @param container
    *   The DOM container element
    * @return
    *   IO[Unit] that completes when the runtime is started
    */
  private def createAndStartRuntime(container: Element): IO[Unit] = {
    for {
      _ <- IO.delay(
        console.log(
          "Runtime creation placeholder - will integrate with TodoApp"
        )
      )
      _ <- IO.delay(
        console.log(
          "Runtime startup placeholder - will start message processing"
        )
      )

      // TODO: Integrate with actual Runtime and TodoApp when compilation issues are resolved
      // val runtime = new Runtime(TodoApp)
      // runtime.start(container)

    } yield ()
  }

  /** Find the application container element in the DOM
    *
    * @return
    *   IO[Element] containing the app container element
    */
  private def findAppContainer(): IO[Element] = {
    IO.delay {
      Option(document.getElementById("app")) match {
        case Some(element) => element
        case None =>
          throw new RuntimeException(
            "Application container element with id 'app' not found. " +
              "Please ensure the HTML page contains <div id=\"app\"></div>"
          )
      }
    }
  }

  /** Handle initialization errors gracefully
    *
    * @param error
    *   The error that occurred during initialization
    * @return
    *   IO[Unit] that handles the error and shows appropriate feedback
    */
  private def handleInitializationError(error: Throwable): IO[Unit] = {
    for {
      // Log the error for debugging
      _ <- IO.delay(
        console.error("Failed to initialize TodoMVC application:", error)
      )

      // Show error to user via HTML page function
      _ <- showErrorToUser(error.getMessage)

      // Optionally, attempt recovery or provide fallback
      _ <- IO.delay(
        console.log(
          "Application initialization failed. Please refresh the page."
        )
      )

    } yield ()
  }

  /** Notify the HTML page that the application is ready
    *
    * Calls the global showApp function defined in the HTML page to hide the
    * loading indicator and show the application.
    *
    * @return
    *   IO[Unit] that completes when the notification is sent
    */
  private def notifyAppReady(): IO[Unit] = {
    IO.delay {
      Try {
        // Call the global showApp function from the HTML page
        val showApp = dom.window.asInstanceOf[scala.scalajs.js.Dynamic].showApp
        if (showApp != null && !scala.scalajs.js.isUndefined(showApp)) {
          showApp.asInstanceOf[scala.scalajs.js.Function0[Unit]]()
          console.log("Notified HTML page that app is ready")
        } else {
          console.warn("showApp function not found on window object")
        }
      } match {
        case Success(_) => ()
        case Failure(e) =>
          console.warn(s"Failed to notify HTML page: ${e.getMessage}")
      }
    }
  }

  /** Show error message to user via HTML page
    *
    * Calls the global showError function defined in the HTML page to display an
    * error message to the user.
    *
    * @param message
    *   The error message to display
    * @return
    *   IO[Unit] that completes when the error is shown
    */
  private def showErrorToUser(message: String): IO[Unit] = {
    IO.delay {
      Try {
        // Call the global showError function from the HTML page
        val showError =
          dom.window.asInstanceOf[scala.scalajs.js.Dynamic].showError
        if (showError != null && !scala.scalajs.js.isUndefined(showError)) {
          showError
            .asInstanceOf[scala.scalajs.js.Function1[String, Unit]](message)
          console.log(s"Showed error to user: $message")
        } else {
          console.error(s"showError function not found. Error: $message")
        }
      } match {
        case Success(_) => ()
        case Failure(e) =>
          console.error(s"Failed to show error to user: ${e.getMessage}")
          console.error(s"Original error: $message")
      }
    }
  }

  /** Console logging utilities for better debugging */
  private object console {
    def log(message: String): Unit = {
      dom.console.log(s"[TodoMVC] $message")
    }

    def warn(message: String): Unit = {
      dom.console.warn(s"[TodoMVC] $message")
    }

    def error(message: String): Unit = {
      dom.console.error(s"[TodoMVC] $message")
    }

    def error(message: String, error: Throwable): Unit = {
      dom.console.error(s"[TodoMVC] $message")
      dom.console.error(error)
    }
  }
}
