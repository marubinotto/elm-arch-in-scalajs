import cats.effect.{IO, IOApp}
import org.scalajs.dom

object Main extends IOApp.Simple {
  def run: IO[Unit] = {
    IO.println("Elm Architecture TodoMVC - Project structure initialized")
  }
}
