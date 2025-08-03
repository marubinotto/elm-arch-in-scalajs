package vdom

import cats.effect.IO

/** HTML element creation functions for virtual DOM
  */
object Html {

  /** Create a div element with attributes and optional events
    */
  def div(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("div", attrs.toMap, Map.empty, children.toList)
  }

  /** Create a div element with attributes and events
    */
  def div(attrs: Map[String, String], events: Map[String, IO[Unit]])(
      children: VNode*
  ): VNode = {
    VElement("div", attrs, events, children.toList)
  }

  /** Create an input element with attributes
    */
  def input(attrs: (String, String)*): VNode = {
    VElement("input", attrs.toMap, Map.empty, List.empty)
  }

  /** Create an input element with attributes and events
    */
  def input(
      attrs: Map[String, String],
      events: Map[String, IO[Unit]]
  ): VNode = {
    VElement("input", attrs, events, List.empty)
  }

  /** Create a button element with attributes
    */
  def button(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("button", attrs.toMap, Map.empty, children.toList)
  }

  /** Create a button element with attributes and events
    */
  def button(attrs: Map[String, String], events: Map[String, IO[Unit]])(
      children: VNode*
  ): VNode = {
    VElement("button", attrs, events, children.toList)
  }

  /** Create a text node
    */
  def text(content: String): VNode = {
    VText(content)
  }

  /** Create a span element
    */
  def span(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("span", attrs.toMap, Map.empty, children.toList)
  }

  /** Create a span element with events
    */
  def span(attrs: Map[String, String], events: Map[String, IO[Unit]])(
      children: VNode*
  ): VNode = {
    VElement("span", attrs, events, children.toList)
  }

  /** Create a ul element
    */
  def ul(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("ul", attrs.toMap, Map.empty, children.toList)
  }

  /** Create a li element
    */
  def li(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("li", attrs.toMap, Map.empty, children.toList)
  }

  /** Create a li element with events
    */
  def li(attrs: Map[String, String], events: Map[String, IO[Unit]])(
      children: VNode*
  ): VNode = {
    VElement("li", attrs, events, children.toList)
  }

  /** Create a label element
    */
  def label(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("label", attrs.toMap, Map.empty, children.toList)
  }

  /** Create a label element with events
    */
  def label(attrs: Map[String, String], events: Map[String, IO[Unit]])(
      children: VNode*
  ): VNode = {
    VElement("label", attrs, events, children.toList)
  }

  /** Create a header element
    */
  def header(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("header", attrs.toMap, Map.empty, children.toList)
  }

  /** Create a section element
    */
  def section(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("section", attrs.toMap, Map.empty, children.toList)
  }

  /** Create a footer element
    */
  def footer(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("footer", attrs.toMap, Map.empty, children.toList)
  }

  /** Create an h1 element
    */
  def h1(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("h1", attrs.toMap, Map.empty, children.toList)
  }

  /** Create an a element
    */
  def a(attrs: (String, String)*)(children: VNode*): VNode = {
    VElement("a", attrs.toMap, Map.empty, children.toList)
  }

  /** Create an a element with attributes and events
    */
  def a(attrs: Map[String, String], events: Map[String, IO[Unit]])(
      children: VNode*
  ): VNode = {
    VElement("a", attrs, events, children.toList)
  }
}
