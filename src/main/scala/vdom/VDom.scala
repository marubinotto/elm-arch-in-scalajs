package vdom

import cats.effect.IO

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
    // Only consider events as changed if keys are added or removed
    val addEvents = newEvents.filter { case (key, _) =>
      !oldEvents.contains(key)
    }
    val removeEvents = oldEvents.keySet -- newEvents.keySet

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
}
