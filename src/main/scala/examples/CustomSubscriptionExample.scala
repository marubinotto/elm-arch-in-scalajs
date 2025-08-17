package examples

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import scala.concurrent.duration._
import architecture._
import vdom._
import vdom.Html._

/** Example demonstrating various custom subscription types */
object CustomSubscriptionExample {

  // Model for the example app
  case class Model(
      counter: Int,
      lastKey: Option[String],
      mousePosition: Option[(Int, Int)],
      wsMessages: List[String],
      customData: String
  )

  // Messages for the example app
  sealed trait Msg
  case object Tick extends Msg
  case class KeyPressed(key: String) extends Msg
  case class MouseClicked(x: Int, y: Int) extends Msg
  case class WebSocketMessage(data: String) extends Msg
  case class WebSocketError(error: String) extends Msg
  case class CustomEvent(data: String) extends Msg

  // Example app using custom subscriptions
  val app = new App[Model, Msg] {

    def init: (Model, Cmd[Msg]) = {
      val initialModel = Model(
        counter = 0,
        lastKey = None,
        mousePosition = None,
        wsMessages = List.empty,
        customData = "Initial"
      )
      (initialModel, Cmd.none)
    }

    def update(msg: Msg, model: Model): Update[Model, Msg] = {
      msg match {
        case Tick =>
          Update(model.copy(counter = model.counter + 1), Cmd.none)

        case KeyPressed(key) =>
          Update(model.copy(lastKey = Some(key)), Cmd.none)

        case MouseClicked(x, y) =>
          Update(model.copy(mousePosition = Some((x, y))), Cmd.none)

        case WebSocketMessage(data) =>
          Update(
            model.copy(wsMessages = data :: model.wsMessages.take(4)),
            Cmd.none
          )

        case WebSocketError(error) =>
          Update(
            model.copy(wsMessages =
              s"Error: $error" :: model.wsMessages.take(4)
            ),
            Cmd.none
          )

        case CustomEvent(data) =>
          Update(model.copy(customData = data), Cmd.none)
      }
    }

    def view(model: Model, dispatch: Option[Msg => IO[Unit]]): VNode = {
      div()(
        h1()(text("Custom Subscriptions Example")),
        div()(
          text(s"Counter (from interval): ${model.counter}")
        ),
        div()(
          text(s"Last key pressed: ${model.lastKey.getOrElse("None")}")
        ),
        div()(
          text(s"Mouse position: ${model.mousePosition
              .map { case (x, y) => s"($x, $y)" }
              .getOrElse("None")}")
        ),
        div()(
          text("WebSocket messages:"),
          ul()(model.wsMessages.map(msg => li()(text(msg)))*)
        ),
        div()(
          text(s"Custom data: ${model.customData}")
        )
      )
    }

    override def subscriptions(model: Model): Sub[Msg] = {
      Sub.batch(
        // Timer subscription - tick every second
        Sub.interval(1.second, Tick),

        // Keyboard subscription - listen for key presses
        Sub.keyboard(key => KeyPressed(key)),

        // Mouse subscription - listen for clicks
        Sub.mouse((x, y) => MouseClicked(x, y)),

        // WebSocket subscription (example URL)
        // Sub.webSocket("ws://localhost:8080/ws",
        //   data => WebSocketMessage(data),
        //   error => WebSocketError(error)
        // ),

        // Custom subscription - example of a custom event source
        Sub.custom(
          "example-custom",
          { dispatch =>
            // Setup: This function sets up the subscription
            // It receives a dispatch function to send messages
            IO.delay {
              // Simulate some custom event source
              val intervalId = org.scalajs.dom.window.setInterval(
                () => {
                  dispatch(
                    CustomEvent(
                      s"Custom event at ${System.currentTimeMillis()}"
                    )
                  ).unsafeRunAndForget()
                },
                3000
              )

              // Return cleanup function
              IO.delay {
                org.scalajs.dom.window.clearInterval(intervalId)
              }
            }
          }
        )
      )
    }
  }
}
