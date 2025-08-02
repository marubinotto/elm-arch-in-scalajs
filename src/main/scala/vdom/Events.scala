package vdom

import cats.effect.IO
import org.scalajs.dom

/** Event handling functions for virtual DOM elements
  */
object Events {

  /** Create an onClick event handler
    */
  def onClick(action: IO[Unit]): (String, IO[Unit]) = {
    "click" -> action
  }

  /** Create an onInput event handler that receives the input value
    */
  def onInput(handler: String => IO[Unit]): (String, IO[Unit]) = {
    "input" -> IO.delay {
      // This will be properly implemented when we have access to the actual event
      // For now, this is a placeholder that will be enhanced in the DOM integration
      handler("").void
    }.flatten
  }

  /** Create an onKeyDown event handler that receives the key code Returns
    * Option[Unit] to allow conditional handling
    */
  def onKeyDown(handler: Int => IO[Option[Unit]]): (String, IO[Unit]) = {
    "keydown" -> IO.delay {
      // This will be properly implemented when we have access to the actual event
      // For now, this is a placeholder that will be enhanced in the DOM integration
      handler(13).flatMap {
        case Some(_) => IO.unit
        case None    => IO.unit
      }.void
    }.flatten
  }

  /** Create an onChange event handler
    */
  def onChange(action: IO[Unit]): (String, IO[Unit]) = {
    "change" -> action
  }

  /** Create an onBlur event handler
    */
  def onBlur(action: IO[Unit]): (String, IO[Unit]) = {
    "blur" -> action
  }

  /** Create an onFocus event handler
    */
  def onFocus(action: IO[Unit]): (String, IO[Unit]) = {
    "focus" -> action
  }

  /** Create an onDoubleClick event handler
    */
  def onDoubleClick(action: IO[Unit]): (String, IO[Unit]) = {
    "dblclick" -> action
  }

  /** Create an onSubmit event handler
    */
  def onSubmit(action: IO[Unit]): (String, IO[Unit]) = {
    "submit" -> action
  }
}
