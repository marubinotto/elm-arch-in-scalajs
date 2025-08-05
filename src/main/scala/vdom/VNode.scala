package vdom

import cats.effect.IO
import org.scalajs.dom.Event

/** Virtual DOM node representation
  */
sealed trait VNode

/** Virtual DOM element with tag, attributes, events, and children
  */
case class VElement(
    tag: String,
    attrs: Map[String, String],
    events: Map[String, Event => IO[Unit]], // Updated to handle real DOM events
    children: List[VNode]
) extends VNode

/** Virtual DOM text node
  */
case class VText(text: String) extends VNode
