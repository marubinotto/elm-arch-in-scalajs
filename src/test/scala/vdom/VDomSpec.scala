package vdom

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO

class VDomSpec extends AnyFunSpec with Matchers {

  describe("VDom.diff") {
    describe("text nodes") {
      it("should return empty patches for identical text nodes") {
        val oldNode = VText("hello")
        val newNode = VText("hello")
        val patches = VDom.diff(oldNode, newNode)
        patches shouldBe empty
      }

      it("should return UpdateText patch for different text") {
        val oldNode = VText("hello")
        val newNode = VText("world")
        val patches = VDom.diff(oldNode, newNode)
        patches should contain only UpdateText("world")
      }
    }

    describe("element nodes") {
      it("should return empty patches for identical elements") {
        val oldNode = VElement("div", Map.empty, Map.empty, List.empty)
        val newNode = VElement("div", Map.empty, Map.empty, List.empty)
        val patches = VDom.diff(oldNode, newNode)
        patches shouldBe empty
      }

      it("should return Replace patch for different tags") {
        val oldNode = VElement("div", Map.empty, Map.empty, List.empty)
        val newNode = VElement("span", Map.empty, Map.empty, List.empty)
        val patches = VDom.diff(oldNode, newNode)
        patches should contain only Replace(newNode)
      }

      it("should return UpdateAttrs patch for different attributes") {
        val oldNode =
          VElement("div", Map("class" -> "old"), Map.empty, List.empty)
        val newNode = VElement(
          "div",
          Map("class" -> "new", "id" -> "test"),
          Map.empty,
          List.empty
        )
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 1
        patches.head shouldBe a[UpdateAttrs]
        val updateAttrs = patches.head.asInstanceOf[UpdateAttrs]
        updateAttrs.addAttrs should contain("class" -> "new")
        updateAttrs.addAttrs should contain("id" -> "test")
        updateAttrs.removeAttrs shouldBe empty
      }

      it("should remove attributes that are no longer present") {
        val oldNode = VElement(
          "div",
          Map("class" -> "old", "id" -> "test"),
          Map.empty,
          List.empty
        )
        val newNode =
          VElement("div", Map("class" -> "new"), Map.empty, List.empty)
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 1
        patches.head shouldBe a[UpdateAttrs]
        val updateAttrs = patches.head.asInstanceOf[UpdateAttrs]
        updateAttrs.addAttrs should contain("class" -> "new")
        updateAttrs.removeAttrs should contain("id")
      }

      it("should return UpdateEvents patch for different events") {
        val oldEvent = IO.unit
        val newEvent = IO.unit
        val oldNode =
          VElement("div", Map.empty, Map("click" -> oldEvent), List.empty)
        val newNode = VElement(
          "div",
          Map.empty,
          Map("click" -> newEvent, "hover" -> newEvent),
          List.empty
        )
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 1
        patches.head shouldBe a[UpdateEvents]
        val updateEvents = patches.head.asInstanceOf[UpdateEvents]
        updateEvents.addEvents should contain key "click"
        updateEvents.addEvents should contain key "hover"
        updateEvents.removeEvents shouldBe empty
      }

      it("should remove events that are no longer present") {
        val event = IO.unit
        val oldNode = VElement(
          "div",
          Map.empty,
          Map("click" -> event, "hover" -> event),
          List.empty
        )
        val newNode =
          VElement("div", Map.empty, Map("click" -> event), List.empty)
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 1
        patches.head shouldBe a[UpdateEvents]
        val updateEvents = patches.head.asInstanceOf[UpdateEvents]
        updateEvents.addEvents should contain key "click"
        updateEvents.removeEvents should contain("hover")
      }
    }

    describe("children diffing") {
      it("should return empty patches for identical children") {
        val child1 = VText("hello")
        val child2 = VText("world")
        val oldNode =
          VElement("div", Map.empty, Map.empty, List(child1, child2))
        val newNode =
          VElement("div", Map.empty, Map.empty, List(child1, child2))
        val patches = VDom.diff(oldNode, newNode)
        patches shouldBe empty
      }

      it("should patch changed children") {
        val oldChild1 = VText("hello")
        val oldChild2 = VText("world")
        val newChild1 = VText("hello")
        val newChild2 = VText("universe")
        val oldNode =
          VElement("div", Map.empty, Map.empty, List(oldChild1, oldChild2))
        val newNode =
          VElement("div", Map.empty, Map.empty, List(newChild1, newChild2))
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 1
        patches.head shouldBe a[PatchChildren]
        val patchChildren = patches.head.asInstanceOf[PatchChildren]
        patchChildren.patches should contain key 1
        patchChildren.patches(1) should contain only UpdateText("universe")
      }

      it("should insert new children") {
        val oldChild = VText("hello")
        val newChild1 = VText("hello")
        val newChild2 = VText("world")
        val oldNode = VElement("div", Map.empty, Map.empty, List(oldChild))
        val newNode =
          VElement("div", Map.empty, Map.empty, List(newChild1, newChild2))
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 1
        patches.head shouldBe InsertChild(1, newChild2)
      }

      it("should remove extra children") {
        val oldChild1 = VText("hello")
        val oldChild2 = VText("world")
        val newChild = VText("hello")
        val oldNode =
          VElement("div", Map.empty, Map.empty, List(oldChild1, oldChild2))
        val newNode = VElement("div", Map.empty, Map.empty, List(newChild))
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 1
        patches.head shouldBe RemoveChild(1)
      }

      it("should handle multiple child operations") {
        val oldChild1 = VText("hello")
        val oldChild2 = VText("world")
        val oldChild3 = VText("foo")
        val newChild1 = VText("hello")
        val newChild2 = VText("universe")
        val newChild3 = VText("bar")
        val newChild4 = VText("baz")

        val oldNode = VElement(
          "div",
          Map.empty,
          Map.empty,
          List(oldChild1, oldChild2, oldChild3)
        )
        val newNode = VElement(
          "div",
          Map.empty,
          Map.empty,
          List(newChild1, newChild2, newChild3, newChild4)
        )
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 2
        patches should contain(InsertChild(3, newChild4))

        val patchChildren = patches
          .find(_.isInstanceOf[PatchChildren])
          .get
          .asInstanceOf[PatchChildren]
        patchChildren.patches should contain key 1
        patchChildren.patches should contain key 2
        patchChildren.patches(1) should contain only UpdateText("universe")
        patchChildren.patches(2) should contain only UpdateText("bar")
      }
    }

    describe("mixed node types") {
      it("should replace text node with element") {
        val oldNode = VText("hello")
        val newNode = VElement("div", Map.empty, Map.empty, List.empty)
        val patches = VDom.diff(oldNode, newNode)
        patches should contain only Replace(newNode)
      }

      it("should replace element with text node") {
        val oldNode = VElement("div", Map.empty, Map.empty, List.empty)
        val newNode = VText("hello")
        val patches = VDom.diff(oldNode, newNode)
        patches should contain only Replace(newNode)
      }
    }

    describe("complex nested structures") {
      it("should handle deeply nested changes") {
        val oldInner =
          VElement("span", Map.empty, Map.empty, List(VText("old")))
        val newInner =
          VElement("span", Map.empty, Map.empty, List(VText("new")))
        val oldNode = VElement("div", Map.empty, Map.empty, List(oldInner))
        val newNode = VElement("div", Map.empty, Map.empty, List(newInner))

        val patches = VDom.diff(oldNode, newNode)
        patches should have length 1
        patches.head shouldBe a[PatchChildren]

        val patchChildren = patches.head.asInstanceOf[PatchChildren]
        patchChildren.patches should contain key 0
        val innerPatches = patchChildren.patches(0)
        innerPatches should have length 1
        innerPatches.head shouldBe a[PatchChildren]

        val innerPatchChildren = innerPatches.head.asInstanceOf[PatchChildren]
        innerPatchChildren.patches should contain key 0
        innerPatchChildren.patches(0) should contain only UpdateText("new")
      }
    }
  }
}
