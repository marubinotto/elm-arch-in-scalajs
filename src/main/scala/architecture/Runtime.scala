package architecture

import cats.effect.{IO, Ref}
import cats.effect.std.Queue
import cats.effect.kernel.Fiber
import cats.syntax.all._
import org.scalajs.dom.Element
import scala.concurrent.duration._
import vdom.{VNode, VDom}

/** Runtime system for managing Elm Architecture applications with concurrent
  * message processing
  *
  * @param app
  *   The application instance implementing the Elm Architecture
  * @tparam Model
  *   The application's model type
  * @tparam Msg
  *   The application's message type
  */
class Runtime[Model, Msg](app: App[Model, Msg]) {

  // Runtime state - will be initialized when start() is called
  private var msgQueueOpt: Option[Queue[IO, Msg]] = None

  /** Start the runtime with the given DOM container element
    *
    * @param container
    *   The DOM element to render the application into
    * @return
    *   IO that completes when the runtime is started
    */
  def start(container: Element): IO[Unit] = {
    val startupIO = for {
      // Validate container element
      _ <- validateContainer(container)

      // Initialize application state with error handling
      initResult <- IO.delay(app.init).handleErrorWith { error =>
        IO.raiseError(
          InitializationError(
            s"App initialization failed: ${error.getMessage}",
            Some(error)
          )
        )
      }
      (initialModel, initialCmd) = initResult

      modelRef <- Ref.of[IO, Model](initialModel)
      msgQueue <- Queue.unbounded[IO, Msg]
      subRef <- Ref.of[IO, Sub[Msg]](Sub.none)
      currentVNodeRef <- Ref.of[IO, Option[VNode]](None)
      errorRef <- Ref.of[IO, Option[AppError]](None)

      // Store message queue for dispatch method
      _ <- IO.delay { msgQueueOpt = Some(msgQueue) }

      // Start all concurrent processes with error handling
      messageProcessor <- processMessagesWithErrorHandling(
        modelRef,
        msgQueue,
        subRef,
        errorRef
      ).start
      renderer <- renderLoopWithErrorHandling(
        modelRef,
        container,
        currentVNodeRef,
        errorRef
      ).start
      subscriptionManager <- subscriptionLoopWithErrorHandling(
        subRef,
        msgQueue,
        errorRef
      ).start

      // Execute initial command with error handling
      _ <- executeCmd(initialCmd, msgQueue).handleErrorWith { error =>
        IO.delay(
          org.scalajs.dom.console
            .error(s"Initial command execution failed: ${error.getMessage}")
        )
      }

      // Store fiber references for potential cleanup (simplified for now)
      _ <- IO.unit
    } yield ()

    // Wrap entire startup in error recovery
    ErrorRecovery.withFallbackIO(
      startupIO,
      IO.delay(
        org.scalajs.dom.console.error("Runtime startup failed completely")
      ),
      logError = true
    )
  }

  /** Validate the container element */
  private def validateContainer(container: Element): IO[Unit] = {
    IO.delay {
      if (container == null) {
        throw InitializationError("Container element is null")
      }
      if (container.parentNode == null) {
        throw InitializationError(
          "Container element is not attached to the DOM"
        )
      }
    }
  }

  /** Process messages from the queue and update the model
    *
    * @param modelRef
    *   Reference to the current model state
    * @param msgQueue
    *   Queue of messages to process
    * @param subRef
    *   Reference to current subscriptions
    * @return
    *   IO that runs the message processing loop
    */
  private def processMessages(
      modelRef: Ref[IO, Model],
      msgQueue: Queue[IO, Msg],
      subRef: Ref[IO, Sub[Msg]]
  ): IO[Unit] = {
    msgQueue.take.flatMap { msg =>
      for {
        currentModel <- modelRef.get
        update = app.update(msg, currentModel)
        _ <- modelRef.set(update.model)
        _ <- executeCmd(update.cmd, msgQueue)
        newSubs = app.subscriptions(update.model)
        _ <- subRef.set(newSubs)
      } yield ()
    }.foreverM
  }

  /** Process messages with comprehensive error handling */
  private def processMessagesWithErrorHandling(
      modelRef: Ref[IO, Model],
      msgQueue: Queue[IO, Msg],
      subRef: Ref[IO, Sub[Msg]],
      errorRef: Ref[IO, Option[AppError]]
  ): IO[Unit] = {
    msgQueue.take.flatMap { msg =>
      val processMessage = for {
        currentModel <- modelRef.get

        // Wrap update function with error boundary
        update <- IO.delay {
          try {
            app.update(msg, currentModel)
          } catch {
            case error: Throwable =>
              org.scalajs.dom.console.error(
                s"Update function error for message $msg: ${error.getMessage}"
              )
              // Return unchanged model with no command on error
              Update(currentModel, Cmd.none)
          }
        }

        // Validate the updated model
        validatedModel <- validateModel(update.model, currentModel)
        _ <- modelRef.set(validatedModel)

        // Execute command with error handling
        _ <- executeCmd(update.cmd, msgQueue).handleErrorWith { error =>
          IO.delay(
            org.scalajs.dom.console.error(
              s"Command execution failed: ${error.getMessage}"
            )
          )
        }

        // Update subscriptions with error handling
        newSubs <- IO.delay(app.subscriptions(validatedModel)).handleErrorWith {
          error =>
            IO.delay(
              org.scalajs.dom.console
                .error(s"Subscription update failed: ${error.getMessage}")
            ) *>
              IO.pure(Sub.none)
        }
        _ <- subRef.set(newSubs)

        // Clear any previous errors on successful processing
        _ <- errorRef.set(None)

      } yield ()

      // Handle any errors in message processing
      processMessage.handleErrorWith { error =>
        val appError = UpdateError(
          s"Message processing failed: ${error.getMessage}",
          msg,
          "model",
          Some(error)
        )
        errorRef.set(Some(appError)) *>
          IO.delay(
            org.scalajs.dom.console.error(
              s"Message processing error: ${appError.message}"
            )
          )
      }
    }.foreverM
  }

  /** Validate model state and recover from invalid states */
  private def validateModel(
      newModel: Model,
      fallbackModel: Model
  ): IO[Model] = {
    IO.delay {
      // Basic validation - in a real app, you'd have more specific validation
      if (newModel == null) {
        org.scalajs.dom.console.warn("Model is null, using fallback")
        fallbackModel
      } else {
        newModel
      }
    }
  }

  /** Render loop that updates the DOM when the model changes
    *
    * @param modelRef
    *   Reference to the current model state
    * @param container
    *   DOM container element
    * @param currentVNodeRef
    *   Reference to the current virtual DOM tree
    * @return
    *   IO that runs the rendering loop
    */
  private def renderLoop(
      modelRef: Ref[IO, Model],
      container: Element,
      currentVNodeRef: Ref[IO, Option[VNode]]
  ): IO[Unit] = {
    for {
      currentModel <- modelRef.get
      newVNode = app.view(currentModel)
      currentVNodeOpt <- currentVNodeRef.get

      _ <- currentVNodeOpt match {
        case None =>
          // Initial render
          for {
            element <- VDom.createElement(newVNode)
            _ <- IO.delay(container.appendChild(element))
            _ <- currentVNodeRef.set(Some(newVNode))
          } yield ()

        case Some(oldVNode) =>
          // Update render
          for {
            patches <- IO.pure(VDom.diff(oldVNode, newVNode))
            _ <-
              if (patches.nonEmpty) {
                for {
                  _ <- VDom.patch(container.firstChild, patches)
                  _ <- currentVNodeRef.set(Some(newVNode))
                } yield ()
              } else IO.unit
          } yield ()
      }

      // Small delay to prevent excessive rendering
      _ <- IO.sleep(16.millis) // ~60 FPS
    } yield ()
  }.foreverM

  /** Render loop with comprehensive error handling */
  private def renderLoopWithErrorHandling(
      modelRef: Ref[IO, Model],
      container: Element,
      currentVNodeRef: Ref[IO, Option[VNode]],
      errorRef: Ref[IO, Option[AppError]]
  ): IO[Unit] = {
    val renderCycle = for {
      currentModel <- modelRef.get

      // Check for errors and render error view if needed
      errorOpt <- errorRef.get

      newVNode <- errorOpt match {
        case Some(error) =>
          // Render error boundary view
          IO.delay(ViewErrorBoundary.defaultFallbackView(currentModel, error))
        case None =>
          // Normal view rendering with error boundary
          IO.delay {
            try {
              app.view(currentModel)
            } catch {
              case error: Throwable =>
                org.scalajs.dom.console
                  .error(s"View function error: ${error.getMessage}")
                ViewErrorBoundary.defaultFallbackView(currentModel, error)
            }
          }
      }

      currentVNodeOpt <- currentVNodeRef.get

      _ <- currentVNodeOpt match {
        case None =>
          // Initial render with error handling
          val initialRender = for {
            element <- VDom.createElement(newVNode).handleErrorWith { error =>
              IO.raiseError(
                DOMError(
                  s"Failed to create initial DOM element: ${error.getMessage}",
                  Some(error)
                )
              )
            }
            _ <- IO.delay(container.appendChild(element)).handleErrorWith {
              error =>
                IO.raiseError(
                  DOMError(
                    s"Failed to append element to container: ${error.getMessage}",
                    Some(error)
                  )
                )
            }
            _ <- currentVNodeRef.set(Some(newVNode))
          } yield ()

          initialRender.handleErrorWith { error =>
            IO.delay(
              org.scalajs.dom.console
                .error(s"Initial render failed: ${error.getMessage}")
            )
          }

        case Some(oldVNode) =>
          // Update render with error handling
          val updateRender = for {
            patches <- IO.delay(VDom.diff(oldVNode, newVNode)).handleErrorWith {
              error =>
                IO.delay(
                  org.scalajs.dom.console
                    .error(s"VDom diff failed: ${error.getMessage}")
                ) *>
                  IO.pure(List.empty) // Empty patches on diff error
            }
            _ <-
              if (patches.nonEmpty) {
                for {
                  _ <- VDom
                    .patch(container.firstChild, patches)
                    .handleErrorWith { error =>
                      IO.delay(
                        org.scalajs.dom.console
                          .error(s"DOM patch failed: ${error.getMessage}")
                      )
                    }
                  _ <- currentVNodeRef.set(Some(newVNode))
                } yield ()
              } else IO.unit
          } yield ()

          updateRender.handleErrorWith { error =>
            IO.delay(
              org.scalajs.dom.console
                .error(s"Update render failed: ${error.getMessage}")
            )
          }
      }

      // Small delay to prevent excessive rendering
      _ <- IO.sleep(16.millis) // ~60 FPS
    } yield ()

    renderCycle.handleErrorWith { error =>
      val renderError =
        RenderError(s"Render cycle failed: ${error.getMessage}", Some(error))
      errorRef.set(Some(renderError)) *>
        IO.delay(
          org.scalajs.dom.console.error(
            s"Render loop error: ${renderError.message}"
          )
        ) *>
        IO.sleep(100.millis) // Longer delay on error to prevent spam
    }.foreverM
  }

  /** Subscription loop that manages external events and timers
    *
    * @param subRef
    *   Reference to current subscriptions
    * @param msgQueue
    *   Queue to send messages to
    * @return
    *   IO that runs the subscription management loop
    */
  private def subscriptionLoop(
      subRef: Ref[IO, Sub[Msg]],
      msgQueue: Queue[IO, Msg]
  ): IO[Unit] = {
    // Keep track of active subscription fibers
    Ref.of[IO, Map[String, Fiber[IO, Throwable, Unit]]](Map.empty).flatMap {
      activeFibersRef =>
        (for {
          currentSub <- subRef.get
          activeFibers <- activeFibersRef.get

          // Cancel all existing subscriptions
          _ <- activeFibers.values.toList.traverse_(_.cancel)
          _ <- activeFibersRef.set(Map.empty)

          // Start new subscriptions
          newFibers <- startSubscriptions(currentSub, msgQueue)
          _ <- activeFibersRef.set(newFibers)

          _ <- IO
            .sleep(100.millis) // Check for subscription changes periodically
        } yield ()).foreverM
    }
  }

  /** Subscription loop with comprehensive error handling */
  private def subscriptionLoopWithErrorHandling(
      subRef: Ref[IO, Sub[Msg]],
      msgQueue: Queue[IO, Msg],
      errorRef: Ref[IO, Option[AppError]]
  ): IO[Unit] = {
    // Keep track of active subscription fibers
    Ref.of[IO, Map[String, Fiber[IO, Throwable, Unit]]](Map.empty).flatMap {
      activeFibersRef =>
        val subscriptionCycle = for {
          currentSub <- subRef.get
          activeFibers <- activeFibersRef.get

          // Cancel all existing subscriptions with error handling
          _ <- activeFibers.values.toList.traverse_ { fiber =>
            fiber.cancel.handleErrorWith { error =>
              IO.delay(
                org.scalajs.dom.console
                  .warn(s"Failed to cancel subscription: ${error.getMessage}")
              )
            }
          }
          _ <- activeFibersRef.set(Map.empty)

          // Start new subscriptions with error handling
          newFibers <- startSubscriptions(currentSub, msgQueue)
            .handleErrorWith { error =>
              IO.delay(
                org.scalajs.dom.console
                  .error(s"Failed to start subscriptions: ${error.getMessage}")
              ) *>
                IO.pure(Map.empty[String, Fiber[IO, Throwable, Unit]])
            }
          _ <- activeFibersRef.set(newFibers)

          _ <- IO
            .sleep(100.millis) // Check for subscription changes periodically
        } yield ()

        subscriptionCycle.handleErrorWith { error =>
          val subError = SubscriptionError(
            s"Subscription management failed: ${error.getMessage}",
            Some(error)
          )
          errorRef.set(Some(subError)) *>
            IO.delay(
              org.scalajs.dom.console
                .error(s"Subscription loop error: ${subError.message}")
            ) *>
            IO.sleep(1000.millis) // Longer delay on error
        }.foreverM
    }
  }

  /** Start subscriptions and return a map of active fibers
    *
    * @param sub
    *   The subscription to start
    * @param msgQueue
    *   Queue to send messages to
    * @return
    *   IO containing a map of subscription identifiers to fibers
    */
  private def startSubscriptions(
      sub: Sub[Msg],
      msgQueue: Queue[IO, Msg]
  ): IO[Map[String, Fiber[IO, Throwable, Unit]]] = {
    sub match {
      case SubNone => IO.pure(Map.empty)

      case SubInterval(duration, msg) =>
        val intervalId = s"interval_${duration.toMillis}_${msg.hashCode}"
        val intervalLoop =
          (IO.sleep(duration) *> msgQueue.offer(msg)).foreverM.void
        intervalLoop.start.map(fiber => Map(intervalId -> fiber))

      case SubBatch(subs) =>
        subs.zipWithIndex
          .traverse { case (s, index) =>
            startSubscriptions(s, msgQueue).map(_.map { case (id, fiber) =>
              (s"batch_${index}_$id", fiber)
            })
          }
          .map(_.flatten.toMap)
    }
  }

  /** Execute a command by processing its side effects
    *
    * @param cmd
    *   The command to execute
    * @param msgQueue
    *   Queue to send resulting messages to
    * @return
    *   IO that completes when the command is executed
    */
  private def executeCmd(cmd: Cmd[Msg], msgQueue: Queue[IO, Msg]): IO[Unit] = {
    cmd match {
      case CmdNone => IO.unit

      case CmdTask(task) =>
        task.flatMap(msg => msgQueue.offer(msg)).start.void

      case CmdBatch(cmds) =>
        cmds.traverse_(executeCmd(_, msgQueue))
    }
  }

  /** Dispatch a message to the application
    *
    * This method can be called from external code to send messages to the
    * application
    *
    * @param msg
    *   The message to dispatch
    * @return
    *   IO that completes when the message is queued
    */
  def dispatch(msg: Msg): IO[Unit] = {
    msgQueueOpt match {
      case Some(queue) =>
        queue.offer(msg).handleErrorWith { error =>
          IO.delay(
            org.scalajs.dom.console
              .error(s"Failed to dispatch message $msg: ${error.getMessage}")
          )
        }
      case None =>
        IO.raiseError(
          InitializationError("Runtime not started - call start() first")
        )
    }
  }

  /** Safe dispatch that doesn't throw errors */
  def safeDispatch(msg: Msg): IO[Unit] = {
    dispatch(msg).handleErrorWith { error =>
      IO.delay(
        org.scalajs.dom.console
          .error(s"Safe dispatch failed for message $msg: ${error.getMessage}")
      )
    }
  }
}

/** Companion object for Runtime with utility functions
  */
object Runtime {

  /** Create and start a new runtime for the given application
    *
    * @param app
    *   The application to run
    * @param container
    *   The DOM container element
    * @tparam Model
    *   The application's model type
    * @tparam Msg
    *   The application's message type
    * @return
    *   IO that completes when the runtime is started
    */
  def run[Model, Msg](app: App[Model, Msg], container: Element): IO[Unit] = {
    new Runtime(app).start(container)
  }
}
