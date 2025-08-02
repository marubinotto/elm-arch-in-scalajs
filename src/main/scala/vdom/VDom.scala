package vdom

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.traverse._
import org.scalajs.dom
import org.scalajs.dom.{Element, Node, Text, HTMLElement, Event}
import scala.scalajs.js

/** Patch operations for updating the DOM
  */
sealed trait Patch

/** Replace an entire node with a new one
  */
case class Replace(newNode: VNode) extends Patch

/** Update the text content of a text node
  */
case class UpdateText(newText: String) extends Patch

/** Update attributes of an element
  */
case class UpdateAttrs(
    addAttrs: Map[String, String],
    removeAttrs: Set[String]
) extends Patch

/** Update event handlers of an element
  */
case class UpdateEvents(
    addEvents: Map[String, IO[Unit]],
    removeEvents: Set[String]
) extends Patch

/** Insert a new child at a specific index
  */
case class InsertChild(index: Int, child: VNode) extends Patch

/** Remove a child at a specific index
  */
case class RemoveChild(index: Int) extends Patch

/** Apply patches to children at specific indices
  */
case class PatchChildren(patches: Map[Int, List[Patch]]) extends Patch

/** Virtual DOM operations
  */
object VDom {

  /** Compare two virtual DOM trees and generate patches
    *
    * @param oldNode
    *   The old virtual DOM tree
    * @param newNode
    *   The new virtual DOM tree
    * @return
    *   List of patches to transform oldNode into newNode
    */
  def diff(oldNode: VNode, newNode: VNode): List[Patch] = {
    diffNodes(oldNode, newNode)
  }

  /** Create a real DOM element from a virtual DOM node
    *
    * @param vnode
    *   The virtual DOM node to create
    * @return
    *   IO containing the created DOM element
    */
  def createElement(vnode: VNode): IO[Node] = {
    vnode match {
      case VText(text) =>
        IO.delay(dom.document.createTextNode(text))

      case VElement(tag, attrs, events, children) =>
        for {
          element <- IO.delay(dom.document.createElement(tag))
          _ <- setAttributes(element, attrs)
          _ <- attachEventListeners(element, events)
          childNodes <- children.traverse(createElement)
          _ <- childNodes.traverse(child =>
            IO.delay(element.appendChild(child))
          )
        } yield element
    }
  }

  /** Apply a list of patches to a DOM element
    *
    * @param element
    *   The DOM element to patch
    * @param patches
    *   The list of patches to apply
    * @return
    *   IO containing the patched element
    */
  def patch(element: Node, patches: List[Patch]): IO[Node] = {
    patches.foldLeft(IO.pure(element)) { (elemIO, patch) =>
      elemIO.flatMap(elem => applyPatch(elem, patch))
    }
  }

  private def diffNodes(oldNode: VNode, newNode: VNode): List[Patch] = {
    (oldNode, newNode) match {
      // Both are text nodes
      case (VText(oldText), VText(newText)) =>
        if (oldText == newText) List.empty
        else List(UpdateText(newText))

      // Both are elements with same tag
      case (
            VElement(oldTag, oldAttrs, oldEvents, oldChildren),
            VElement(newTag, newAttrs, newEvents, newChildren)
          ) if oldTag == newTag =>
        val attrPatches = diffAttrs(oldAttrs, newAttrs)
        val eventPatches = diffEvents(oldEvents, newEvents)
        val childPatches = diffChildren(oldChildren, newChildren)

        attrPatches ++ eventPatches ++ childPatches

      // Different node types or different tags - replace entirely
      case _ =>
        List(Replace(newNode))
    }
  }

  private def diffAttrs(
      oldAttrs: Map[String, String],
      newAttrs: Map[String, String]
  ): List[Patch] = {
    val addAttrs = newAttrs.filter { case (key, value) =>
      oldAttrs.get(key) != Some(value)
    }
    val removeAttrs = oldAttrs.keySet -- newAttrs.keySet

    if (addAttrs.nonEmpty || removeAttrs.nonEmpty) {
      List(UpdateAttrs(addAttrs, removeAttrs))
    } else {
      List.empty
    }
  }

  private def diffEvents(
      oldEvents: Map[String, IO[Unit]],
      newEvents: Map[String, IO[Unit]]
  ): List[Patch] = {
    // For events, we compare by key presence since IO[Unit] doesn't have meaningful equality
    // We need to update all events in newEvents and remove events not in newEvents
    // However, if the event sets are identical by keys, we assume no change
    val removeEvents = oldEvents.keySet -- newEvents.keySet
    val addEvents =
      if (oldEvents.keySet == newEvents.keySet && removeEvents.isEmpty) {
        // If the keys are the same, assume no change needed
        Map.empty[String, IO[Unit]]
      } else {
        // Otherwise, update all events in newEvents
        newEvents
      }

    if (addEvents.nonEmpty || removeEvents.nonEmpty) {
      List(UpdateEvents(addEvents, removeEvents))
    } else {
      List.empty
    }
  }

  private def diffChildren(
      oldChildren: List[VNode],
      newChildren: List[VNode]
  ): List[Patch] = {
    val oldLength = oldChildren.length
    val newLength = newChildren.length
    val minLength = math.min(oldLength, newLength)

    // Diff existing children
    val childPatches = (0 until minLength).flatMap { index =>
      val patches = diffNodes(oldChildren(index), newChildren(index))
      if (patches.nonEmpty) {
        Some(index -> patches)
      } else {
        None
      }
    }.toMap

    // Handle length differences
    val lengthPatches = if (newLength > oldLength) {
      // Insert new children
      (oldLength until newLength).map { index =>
        InsertChild(index, newChildren(index))
      }.toList
    } else if (newLength < oldLength) {
      // Remove extra children (remove from the end to maintain indices)
      (newLength until oldLength).reverse.map { index =>
        RemoveChild(index)
      }.toList
    } else {
      List.empty
    }

    val patches = if (childPatches.nonEmpty) {
      List(PatchChildren(childPatches))
    } else {
      List.empty
    }

    patches ++ lengthPatches
  }

  /** Apply a single patch to a DOM element
    */
  private def applyPatch(element: Node, patch: Patch): IO[Node] = {
    patch match {
      case Replace(newNode) =>
        for {
          newElement <- createElement(newNode)
          parent <- IO.delay(element.parentNode)
          _ <- IO.delay(parent.replaceChild(newElement, element))
        } yield newElement

      case UpdateText(newText) =>
        element match {
          case textNode: Text =>
            IO.delay {
              textNode.textContent = newText
              textNode
            }
          case _ =>
            IO.raiseError(
              new IllegalArgumentException(
                "Cannot update text on non-text node"
              )
            )
        }

      case UpdateAttrs(addAttrs, removeAttrs) =>
        element match {
          case elem: Element =>
            for {
              _ <- removeAttrs.toList.traverse(attr =>
                IO.delay(elem.removeAttribute(attr))
              )
              _ <- setAttributes(elem, addAttrs)
            } yield elem
          case _ =>
            IO.raiseError(
              new IllegalArgumentException(
                "Cannot update attributes on non-element node"
              )
            )
        }

      case UpdateEvents(addEvents, removeEvents) =>
        element match {
          case elem: Element =>
            for {
              _ <- removeEventListeners(elem, removeEvents)
              _ <- attachEventListeners(elem, addEvents)
            } yield elem
          case _ =>
            IO.raiseError(
              new IllegalArgumentException(
                "Cannot update events on non-element node"
              )
            )
        }

      case InsertChild(index, child) =>
        for {
          childElement <- createElement(child)
          _ <- IO.delay {
            val children = element.childNodes
            if (index >= children.length) {
              element.appendChild(childElement)
            } else {
              element.insertBefore(childElement, children(index))
            }
          }
        } yield element

      case RemoveChild(index) =>
        IO.delay {
          val children = element.childNodes
          if (index < children.length) {
            element.removeChild(children(index))
          }
          element
        }

      case PatchChildren(childPatches) =>
        childPatches.toList
          .traverse { case (index, patches) =>
            IO.delay(element.childNodes(index))
              .flatMap(child => VDom.patch(child, patches))
          }
          .map(_ => element)
    }
  }

  /** Set attributes on a DOM element
    */
  private def setAttributes(
      element: Element,
      attrs: Map[String, String]
  ): IO[Unit] = {
    attrs.toList.traverse { case (name, value) =>
      IO.delay(element.setAttribute(name, value))
    }.void
  }

  /** Attach event listeners to a DOM element
    */
  private def attachEventListeners(
      element: Element,
      events: Map[String, IO[Unit]]
  ): IO[Unit] = {
    events.toList.traverse { case (eventType, handler) =>
      IO.delay {
        val listener: js.Function1[Event, Unit] = (event: Event) => {
          // Handle special cases for input events
          eventType match {
            case "input" =>
              val target = event.target.asInstanceOf[dom.HTMLInputElement]
              val value = target.value
              // For input events, we need to create a new handler that passes the value
              // This is a simplified approach - in a real implementation, we'd need better event handling
              handler.unsafeRunAsync(_ => ())
            case "keydown" =>
              val keyEvent = event.asInstanceOf[dom.KeyboardEvent]
              val keyCode = keyEvent.keyCode.toInt
              // Similar to input, we'd need better event handling for keydown
              handler.unsafeRunAsync(_ => ())
            case _ =>
              handler.unsafeRunAsync(_ => ())
          }
        }
        element.addEventListener(eventType, listener)
        // Store the listener for later removal (in a real implementation, we'd need a registry)
        element
          .asInstanceOf[js.Dynamic]
          .updateDynamic(s"__${eventType}_listener")(listener)
      }
    }.void
  }

  /** Remove event listeners from a DOM element
    */
  private def removeEventListeners(
      element: Element,
      eventTypes: Set[String]
  ): IO[Unit] = {
    eventTypes.toList.traverse { eventType =>
      IO.delay {
        val listener = element
          .asInstanceOf[js.Dynamic]
          .selectDynamic(s"__${eventType}_listener")
        if (!js.isUndefined(listener)) {
          element.removeEventListener(
            eventType,
            listener.asInstanceOf[js.Function1[Event, Unit]]
          )
          element
            .asInstanceOf[js.Dynamic]
            .updateDynamic(s"__${eventType}_listener")(js.undefined)
        }
      }
    }.void
  }
}
