package vdom

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO

class VNodeSpec extends AnyFunSpec with Matchers {

  describe("VNode types") {
    describe("VText") {
      it("should create a text node with content") {
        val textNode = VText("Hello World")
        textNode.text shouldBe "Hello World"
      }

      it("should handle empty text") {
        val textNode = VText("")
        textNode.text shouldBe ""
      }

      it("should handle special characters") {
        val textNode = VText("Hello & <World>")
        textNode.text shouldBe "Hello & <World>"
      }
    }

    describe("VElement") {
      it(
        "should create an element with tag, attributes, events, and children"
      ) {
        val events = Map("click" -> IO.unit)
        val attrs = Map("class" -> "test", "id" -> "element1")
        val children = List(VText("Child text"))

        val element = VElement("div", attrs, events, children)

        element.tag shouldBe "div"
        element.attrs shouldBe attrs
        element.events shouldBe events
        element.children shouldBe children
      }

      it("should create an element with no attributes") {
        val element = VElement("span", Map.empty, Map.empty, List.empty)

        element.tag shouldBe "span"
        element.attrs shouldBe Map.empty
        element.events shouldBe Map.empty
        element.children shouldBe List.empty
      }

      it("should create an element with nested children") {
        val innerChild =
          VElement("span", Map.empty, Map.empty, List(VText("Inner")))
        val outerChild = VElement("div", Map.empty, Map.empty, List(innerChild))
        val root = VElement("section", Map.empty, Map.empty, List(outerChild))

        root.tag shouldBe "section"
        root.children.length shouldBe 1
        root.children.head shouldBe outerChild
      }
    }
  }
}
