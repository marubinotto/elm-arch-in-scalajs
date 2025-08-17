package examples

import architecture._
import cats.effect.IO
import vdom.VNode
import vdom.Html._
import scala.concurrent.duration._

/** Example demonstrating the refactored subscription management system
  */
object SubscriptionExample {

  // Example model and messages
  case class CounterModel(count: Int, isRunning: Boolean)

  sealed trait CounterMsg
  case object Increment extends CounterMsg
  case object Toggle extends CounterMsg
  case class KeyPressed(key: String) extends CounterMsg

  // Example app using the new subscription system
  val counterApp = new App[CounterModel, CounterMsg] {

    def init: (CounterModel, Cmd[CounterMsg]) = {
      (CounterModel(0, false), Cmd.none)
    }

    def update(
        msg: CounterMsg,
        model: CounterModel
    ): Update[CounterModel, CounterMsg] = {
      msg match {
        case Increment =>
          Update(model.copy(count = model.count + 1), Cmd.none)

        case Toggle =>
          Update(model.copy(isRunning = !model.isRunning), Cmd.none)

        case KeyPressed(key) =>
          if (key == " ") { // Space bar toggles
            Update(model.copy(isRunning = !model.isRunning), Cmd.none)
          } else {
            Update(model, Cmd.none)
          }
      }
    }

    def view(
        model: CounterModel,
        dispatch: Option[CounterMsg => IO[Unit]]
    ): VNode = {
      div("class" -> "counter-example")(
        h1()(text("Subscription Management Example")),
        div()(text(s"Count: ${model.count}")),
        div()(
          text(s"Status: ${if (model.isRunning) "Running" else "Stopped"}")
        ),
        div()(text("Press spacebar to toggle, or click the button")),
        dispatch match {
          case Some(dispatchFn) =>
            button(
              Map("type" -> "button"),
              Map("click" -> { (_: org.scalajs.dom.Event) =>
                dispatchFn(Toggle)
              })
            )(text(if (model.isRunning) "Stop" else "Start"))
          case None =>
            button("type" -> "button")(text("Toggle"))
        }
      )
    }

    override def subscriptions(model: CounterModel): Sub[CounterMsg] = {
      if (model.isRunning) {
        // When running, combine interval and keyboard subscriptions
        Sub.batch(
          Sub.interval(1.second, Increment),
          Sub.keyboard(KeyPressed.apply)
        )
      } else {
        // When stopped, only listen for keyboard
        Sub.keyboard(KeyPressed.apply)
      }
    }
  }

  /** Demonstrates the benefits of the new subscription system:
    *
    *   1. **Efficient Updates**: Only changes what's different between
    *      subscription states 2. **Proper Cleanup**: Event listeners are
    *      properly removed when subscriptions change 3. **Type Safety**:
    *      Compile-time guarantees about subscription structure 4.
    *      **Composability**: Easy to combine different subscription types 5.
    *      **Debugging**: Clear subscription IDs and status information
    *
    * The old system would:
    *   - Cancel ALL subscriptions and restart them on every model change
    *   - Have complex manual fiber management with string-based IDs
    *   - Mix error handling throughout the subscription logic
    *   - Be tightly coupled to the Runtime class
    *
    * The new system:
    *   - Only updates subscriptions that actually changed (diffing)
    *   - Has clean separation of concerns with dedicated SubscriptionManager
    *   - Provides stable, predictable subscription IDs
    *   - Handles cleanup automatically and safely
    *   - Is much easier to test and debug
    */
}
