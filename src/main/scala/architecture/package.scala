import cats.effect.IO
import scala.concurrent.duration.FiniteDuration

// Core Elm Architecture types and abstractions
package object architecture {

  // Update case class for model and command results
  case class Update[Model, Msg](model: Model, cmd: Cmd[Msg])

  // App trait with init, update, view, and subscriptions methods
  trait App[Model, Msg] {
    def init: (Model, Cmd[Msg])
    def update(msg: Msg, model: Model): Update[Model, Msg]
    def view(model: Model, dispatch: Option[Msg => IO[Unit]] = None): vdom.VNode
    def subscriptions(model: Model): Sub[Msg] = Sub.none
  }

  // Cmd sealed trait with variants
  sealed trait Cmd[+Msg]
  case object CmdNone extends Cmd[Nothing]
  case class CmdBatch[Msg](cmds: List[Cmd[Msg]]) extends Cmd[Msg]
  case class CmdTask[Msg](task: IO[Msg]) extends Cmd[Msg]

  // Sub sealed trait with variants
  sealed trait Sub[+Msg]
  case object SubNone extends Sub[Nothing]
  case class SubBatch[Msg](subs: List[Sub[Msg]]) extends Sub[Msg]
  case class SubInterval[Msg](duration: FiniteDuration, msg: Msg)
      extends Sub[Msg]

  // Cmd companion object with helper functions
  object Cmd {
    def none[Msg]: Cmd[Msg] = CmdNone
    def task[Msg](io: IO[Msg]): Cmd[Msg] = CmdTask(io)
    def batch[Msg](cmds: Cmd[Msg]*): Cmd[Msg] = CmdBatch(cmds.toList)
  }

  // Sub companion object with helper functions
  object Sub {
    def none[Msg]: Sub[Msg] = SubNone
    def interval[Msg](duration: FiniteDuration, msg: Msg): Sub[Msg] =
      SubInterval(duration, msg)
    def batch[Msg](subs: Sub[Msg]*): Sub[Msg] = SubBatch(subs.toList)
  }
}
