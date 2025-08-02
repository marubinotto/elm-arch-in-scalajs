import architecture._
import vdom._
import cats.effect.IO
import scala.concurrent.duration._

// Simple example to demonstrate the architecture types work
object ArchitectureExample {

  // Example model and message types
  case class ExampleModel(value: String)
  sealed trait ExampleMsg
  case class SetValue(newValue: String) extends ExampleMsg

  // Example app implementation
  val exampleApp: App[ExampleModel, ExampleMsg] =
    new App[ExampleModel, ExampleMsg] {
      def init: (ExampleModel, Cmd[ExampleMsg]) =
        (ExampleModel("initial"), Cmd.none)

      def update(
          msg: ExampleMsg,
          model: ExampleModel
      ): Update[ExampleModel, ExampleMsg] =
        msg match {
          case SetValue(newValue) =>
            Update(model.copy(value = newValue), Cmd.none)
        }

      def view(model: ExampleModel): VNode =
        Html.div()(Html.text(s"Value: ${model.value}"))
    }

  // Example usage of Cmd types
  val cmdExamples: List[Cmd[ExampleMsg]] = List(
    Cmd.none,
    Cmd.task(IO.pure(SetValue("from task"))),
    Cmd.batch(Cmd.none, Cmd.task(IO.pure(SetValue("batched"))))
  )

  // Example usage of Sub types
  val subExamples: List[Sub[ExampleMsg]] = List(
    Sub.none,
    Sub.interval(1.second, SetValue("from interval")),
    Sub.batch(Sub.none, Sub.interval(2.seconds, SetValue("batched")))
  )
}
