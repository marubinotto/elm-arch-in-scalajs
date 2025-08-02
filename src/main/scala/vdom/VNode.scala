package vdom

import cats.effect.IO

/** Virtual DOM node representation
  */
sealed trait VNode

/** Virtual DOM element with tag, attributes, events, and children
  */
case class VElement(
    tag: String,
    attrs: Map[String, String],
    events: Map[String, IO[
      Unit
    ]], // Simplified for now, will be parameterized later
    children: List[VNode]
) extends VNode

/** Virtual DOM text node
  */
case class VText(text: String) extends VNode
