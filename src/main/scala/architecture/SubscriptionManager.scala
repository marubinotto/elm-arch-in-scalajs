package architecture

import cats.effect.{IO, Ref, Fiber}
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import org.scalajs.dom
import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js

/** Manages the lifecycle of subscriptions with efficient updates and proper
  * cleanup
  *
  * @tparam Msg
  *   The message type for the application
  */
class SubscriptionManager[Msg] private (
    activeFibersRef: Ref[IO, Map[String, ActiveSubscription[Msg]]],
    msgQueue: Queue[IO, Msg]
) {

  /** Update subscriptions by diffing old and new subscription trees
    *
    * This is more efficient than canceling all and restarting, as it only
    * changes what's actually different.
    *
    * @param newSub
    *   The new subscription tree
    * @return
    *   IO that completes when subscriptions are updated
    */
  def updateSubscriptions(newSub: Sub[Msg]): IO[Unit] = {
    for {
      currentFibers <- activeFibersRef.get
      newSubMap = flattenSubscriptions(newSub)

      // Find subscriptions to remove (in current but not in new)
      toRemove = currentFibers.keySet -- newSubMap.keySet

      // Find subscriptions to add (in new but not in current, or changed)
      toAdd = newSubMap.filter { case (id, sub) =>
        currentFibers.get(id).forall(_.subscription != sub)
      }

      // Remove old subscriptions
      _ <- toRemove.toList.traverse_(removeSubscription)

      // Add new subscriptions
      _ <- toAdd.toList.traverse { case (id, sub) =>
        startSubscription(id, sub)
      }
    } yield ()
  }

  /** Shutdown all active subscriptions */
  def shutdown(): IO[Unit] = {
    for {
      fibers <- activeFibersRef.get
      _ <- fibers.values.toList.traverse_(_.cleanup)
      _ <- activeFibersRef.set(Map.empty)
    } yield ()
  }

  /** Get current subscription status for debugging */
  def getStatus(): IO[SubscriptionStatus] = {
    activeFibersRef.get.map { fibers =>
      SubscriptionStatus(
        activeCount = fibers.size,
        subscriptionIds = fibers.keys.toList.sorted
      )
    }
  }

  // Private implementation methods

  /** Convert a subscription tree into a flat map with unique IDs */
  private def flattenSubscriptions(sub: Sub[Msg]): Map[String, Sub[Msg]] = {
    def flatten(s: Sub[Msg], prefix: String = ""): Map[String, Sub[Msg]] = {
      s match {
        case SubNone => Map.empty

        case SubBatch(subs) =>
          subs.zipWithIndex.flatMap { case (subSub, index) =>
            flatten(subSub, s"${prefix}batch_$index.")
          }.toMap

        case other =>
          val id = generateSubscriptionId(other, prefix)
          Map(id -> other)
      }
    }
    flatten(sub)
  }

  /** Generate a stable ID for a subscription */
  private def generateSubscriptionId(sub: Sub[Msg], prefix: String): String = {
    val baseId = sub match {
      case SubInterval(duration, _) => s"interval_${duration.toMillis}"
      case SubKeyboard(_)           => "keyboard"
      case SubMouse(_)              => "mouse"
      case SubWebSocket(url, _, _)  => s"websocket_${url.hashCode}"
      case SubCustom(id, _)         => s"custom_$id"
      case _                        => "unknown"
    }
    s"$prefix$baseId"
  }

  /** Start a single subscription */
  private def startSubscription(id: String, sub: Sub[Msg]): IO[Unit] = {
    for {
      activeSub <- createActiveSubscription(sub)
      _ <- activeFibersRef.update(_ + (id -> activeSub))
    } yield ()
  }

  /** Remove a single subscription */
  private def removeSubscription(id: String): IO[Unit] = {
    for {
      currentFibers <- activeFibersRef.get
      _ <- currentFibers.get(id).traverse_(_.cleanup)
      _ <- activeFibersRef.update(_ - id)
    } yield ()
  }

  /** Create an active subscription from a subscription definition */
  private def createActiveSubscription(
      sub: Sub[Msg]
  ): IO[ActiveSubscription[Msg]] = {
    sub match {
      case SubInterval(duration, msg) =>
        createIntervalSubscription(duration, msg)

      case SubKeyboard(onKeyDown) =>
        createKeyboardSubscription(onKeyDown)

      case SubMouse(onClick) =>
        createMouseSubscription(onClick)

      case SubWebSocket(url, onMessage, onError) =>
        createWebSocketSubscription(url, onMessage, onError)

      case SubCustom(_, setup) =>
        createCustomSubscription(setup)

      case _ =>
        IO.raiseError(
          new IllegalArgumentException(
            s"Cannot create active subscription for: $sub"
          )
        )
    }
  }

  /** Create an interval subscription */
  private def createIntervalSubscription(
      duration: FiniteDuration,
      msg: Msg
  ): IO[ActiveSubscription[Msg]] = {
    val loop = (IO.sleep(duration) *> msgQueue.offer(msg)).foreverM.void
    loop.start.map { fiber =>
      ActiveSubscription(
        subscription = SubInterval(duration, msg),
        fiber = fiber,
        cleanup = fiber.cancel.void
      )
    }
  }

  /** Create a keyboard subscription */
  private def createKeyboardSubscription(
      onKeyDown: String => Msg
  ): IO[ActiveSubscription[Msg]] = {
    for {
      listenerRef <- Ref.of[IO, Option[js.Function1[dom.KeyboardEvent, Unit]]](
        None
      )

      setupIO = IO.delay {
        val listener: js.Function1[dom.KeyboardEvent, Unit] = { event =>
          val msg = onKeyDown(event.key)
          msgQueue.offer(msg).unsafeRunAndForget()
        }
        dom.document.addEventListener("keydown", listener)
        listener
      }

      cleanupIO = listenerRef.get.flatMap {
        case Some(listener) =>
          IO.delay(dom.document.removeEventListener("keydown", listener))
        case None => IO.unit
      }

      listener <- setupIO
      _ <- listenerRef.set(Some(listener))

      // Create a fiber that just waits (the real work is done by the event listener)
      fiber <- IO.never[Unit].start

    } yield ActiveSubscription(
      subscription = SubKeyboard(onKeyDown),
      fiber = fiber,
      cleanup = cleanupIO *> fiber.cancel.void
    )
  }

  /** Create a mouse subscription */
  private def createMouseSubscription(
      onClick: (Int, Int) => Msg
  ): IO[ActiveSubscription[Msg]] = {
    for {
      listenerRef <- Ref.of[IO, Option[js.Function1[dom.MouseEvent, Unit]]](
        None
      )

      setupIO = IO.delay {
        val listener: js.Function1[dom.MouseEvent, Unit] = { event =>
          val msg = onClick(event.clientX.toInt, event.clientY.toInt)
          msgQueue.offer(msg).unsafeRunAndForget()
        }
        dom.document.addEventListener("click", listener)
        listener
      }

      cleanupIO = listenerRef.get.flatMap {
        case Some(listener) =>
          IO.delay(dom.document.removeEventListener("click", listener))
        case None => IO.unit
      }

      listener <- setupIO
      _ <- listenerRef.set(Some(listener))
      fiber <- IO.never[Unit].start

    } yield ActiveSubscription(
      subscription = SubMouse(onClick),
      fiber = fiber,
      cleanup = cleanupIO *> fiber.cancel.void
    )
  }

  /** Create a WebSocket subscription */
  private def createWebSocketSubscription(
      url: String,
      onMessage: String => Msg,
      onError: String => Msg
  ): IO[ActiveSubscription[Msg]] = {
    for {
      wsRef <- Ref.of[IO, Option[dom.WebSocket]](None)

      setupIO = IO.delay {
        val ws = new dom.WebSocket(url)

        ws.onmessage = { event =>
          val msg = onMessage(event.data.toString)
          msgQueue.offer(msg).unsafeRunAndForget()
        }

        ws.onerror = { _ =>
          val msg = onError(s"WebSocket error for $url")
          msgQueue.offer(msg).unsafeRunAndForget()
        }

        ws
      }

      cleanupIO = wsRef.get.flatMap {
        case Some(ws) =>
          IO.delay {
            if (
              ws.readyState == dom.WebSocket.OPEN || ws.readyState == dom.WebSocket.CONNECTING
            ) {
              ws.close()
            }
          }
        case None => IO.unit
      }

      ws <- setupIO
      _ <- wsRef.set(Some(ws))
      fiber <- IO.never[Unit].start

    } yield ActiveSubscription(
      subscription = SubWebSocket(url, onMessage, onError),
      fiber = fiber,
      cleanup = cleanupIO *> fiber.cancel.void
    )
  }

  /** Create a custom subscription */
  private def createCustomSubscription(
      setup: (Msg => IO[Unit]) => IO[IO[Unit]]
  ): IO[ActiveSubscription[Msg]] = {
    val dispatch = (msg: Msg) => msgQueue.offer(msg)

    for {
      customCleanup <- setup(dispatch)
      fiber <- IO.never[Unit].start
    } yield ActiveSubscription(
      subscription = SubCustom("", setup), // ID will be set by caller
      fiber = fiber,
      cleanup = customCleanup *> fiber.cancel.void
    )
  }
}

/** Represents an active subscription with its cleanup mechanism */
private case class ActiveSubscription[Msg](
    subscription: Sub[Msg],
    fiber: Fiber[IO, Throwable, Unit],
    cleanup: IO[Unit]
)

/** Status information about active subscriptions */
case class SubscriptionStatus(
    activeCount: Int,
    subscriptionIds: List[String]
)

object SubscriptionManager {

  /** Create a new subscription manager */
  def create[Msg](msgQueue: Queue[IO, Msg]): IO[SubscriptionManager[Msg]] = {
    Ref.of[IO, Map[String, ActiveSubscription[Msg]]](Map.empty).map { ref =>
      new SubscriptionManager(ref, msgQueue)
    }
  }
}
