package vdom

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalajs.dom
import org.scalajs.dom.{Event, HTMLInputElement, KeyboardEvent}
import scala.scalajs.js

/** Event handling functions for virtual DOM elements with real DOM event
  * capture
  */
object Events {

  /** Create an onClick event handler
    */
  def onClick(action: IO[Unit]): (String, Event => IO[Unit]) = {
    "click" -> { (_: Event) => action }
  }

  /** Create an onInput event handler that receives the input value This
    * properly extracts the value from the DOM event
    */
  def onInput(handler: String => IO[Unit]): (String, Event => IO[Unit]) = {
    "input" -> { (event: Event) =>
      IO.delay {
        val target = event.target.asInstanceOf[HTMLInputElement]
        target.value
      }.flatMap(handler)
    }
  }

  /** Create an onKeyDown event handler that receives the key code Returns
    * Option[Unit] to allow conditional handling
    */
  def onKeyDown(
      handler: Int => IO[Option[Unit]]
  ): (String, Event => IO[Unit]) = {
    "keydown" -> { (event: Event) =>
      IO.delay {
        val keyEvent = event.asInstanceOf[KeyboardEvent]
        keyEvent.keyCode
      }.flatMap(handler)
        .flatMap {
          case Some(_) => IO.unit
          case None    => IO.unit
        }
    }
  }

  /** Create an onChange event handler
    */
  def onChange(action: IO[Unit]): (String, Event => IO[Unit]) = {
    "change" -> { (_: Event) => action }
  }

  /** Create an onBlur event handler
    */
  def onBlur(action: IO[Unit]): (String, Event => IO[Unit]) = {
    "blur" -> { (_: Event) => action }
  }

  /** Create an onFocus event handler
    */
  def onFocus(action: IO[Unit]): (String, Event => IO[Unit]) = {
    "focus" -> { (_: Event) => action }
  }

  /** Create an onDoubleClick event handler
    */
  def onDoubleClick(action: IO[Unit]): (String, Event => IO[Unit]) = {
    "dblclick" -> { (_: Event) => action }
  }

  /** Create an onSubmit event handler with preventDefault
    */
  def onSubmit(action: IO[Unit]): (String, Event => IO[Unit]) = {
    "submit" -> { (event: Event) =>
      IO.delay(event.preventDefault()) *> action
    }
  }

  /** Create an onKeyDown event handler for Enter key specifically
    */
  def onEnterKey(action: IO[Unit]): (String, Event => IO[Unit]) = {
    "keydown" -> { (event: Event) =>
      IO.delay {
        val keyEvent = event.asInstanceOf[KeyboardEvent]
        if (keyEvent.keyCode == 13) { // Enter key
          keyEvent.preventDefault()
          action.unsafeRunAsync(_ => ())
        }
      }
    }
  }

  /** Create an onKeyDown event handler for Escape key specifically
    */
  def onEscapeKey(action: IO[Unit]): (String, Event => IO[Unit]) = {
    "keydown" -> { (event: Event) =>
      IO.delay {
        val keyEvent = event.asInstanceOf[KeyboardEvent]
        if (keyEvent.keyCode == 27) { // Escape key
          keyEvent.preventDefault()
          action.unsafeRunAsync(_ => ())
        }
      }
    }
  }

  /** Create an onKeyDown event handler that handles both Enter and Escape
    */
  def onEditKeyDown(
      onEnter: IO[Unit],
      onEscape: IO[Unit]
  ): (String, Event => IO[Unit]) = {
    "keydown" -> { (event: Event) =>
      IO.delay {
        val keyEvent = event.asInstanceOf[KeyboardEvent]
        keyEvent.keyCode match {
          case 13 => // Enter key
            keyEvent.preventDefault()
            onEnter.unsafeRunAsync(_ => ())
          case 27 => // Escape key
            keyEvent.preventDefault()
            onEscape.unsafeRunAsync(_ => ())
          case _ => // Other keys - do nothing
        }
      }
    }
  }

  /** Create an onInput event handler that extracts value and creates a message
    */
  def onInputValue[Msg](
      msgFactory: String => Msg,
      dispatch: Msg => IO[Unit]
  ): (String, Event => IO[Unit]) = {
    "input" -> { (event: Event) =>
      for {
        target <- IO.delay(event.target.asInstanceOf[HTMLInputElement])
        value <- IO.delay(target.value)
        _ <- dispatch(msgFactory(value))
      } yield ()
    }
  }

  /** Create an onClick event handler that dispatches a message
    */
  def onClickMsg[Msg](
      msg: Msg,
      dispatch: Msg => IO[Unit]
  ): (String, Event => IO[Unit]) = {
    "click" -> { (event: Event) =>
      for {
        _ <- IO.delay(event.preventDefault())
        _ <- dispatch(msg)
      } yield ()
    }
  }

  /** Create an onChange event handler that dispatches a message
    */
  def onChangeMsg[Msg](
      msg: Msg,
      dispatch: Msg => IO[Unit]
  ): (String, Event => IO[Unit]) = {
    "change" -> { (_: Event) =>
      dispatch(msg)
    }
  }

  /** Create an onDoubleClick event handler that dispatches a message
    */
  def onDoubleClickMsg[Msg](
      msg: Msg,
      dispatch: Msg => IO[Unit]
  ): (String, Event => IO[Unit]) = {
    "dblclick" -> { (event: Event) =>
      for {
        _ <- IO.delay(event.preventDefault())
        _ <- dispatch(msg)
      } yield ()
    }
  }

  /** Create an onBlur event handler that extracts input value and dispatches a
    * message
    */
  def onBlurValue[Msg](
      msgFactory: String => Msg,
      dispatch: Msg => IO[Unit]
  ): (String, Event => IO[Unit]) = {
    "blur" -> { (event: Event) =>
      for {
        target <- IO.delay(event.target.asInstanceOf[HTMLInputElement])
        value <- IO.delay(target.value)
        _ <- dispatch(msgFactory(value))
      } yield ()
    }
  }
}
