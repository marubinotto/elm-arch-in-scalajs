package vdom

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO

class HtmlSpec extends AnyFunSpec with Matchers {

  describe("Html object") {
    describe("div") {
      it("should create a div element with attributes") {
        val divNode = Html.div("class" -> "container", "id" -> "main")(
          Html.text("Content")
        )

        divNode shouldBe a[VElement]
        val element = divNode.asInstanceOf[VElement]
        element.tag shouldBe "div"
        element.attrs shouldBe Map("class" -> "container", "id" -> "main")
        element.children.length shouldBe 1
        element.children.head shouldBe VText("Content")
      }

      it("should create a div element with events") {
        val clickHandler = IO.unit
        val events = Map("click" -> clickHandler)
        val attrs = Map("class" -> "clickable")

        val divNode = Html.div(attrs, events)(Html.text("Click me"))

        val element = divNode.asInstanceOf[VElement]
        element.tag shouldBe "div"
        element.attrs shouldBe attrs
        element.events shouldBe events
        element.children.length shouldBe 1
      }

      it("should create a div with multiple children") {
        val divNode = Html.div()(
          Html.text("First child"),
          Html.span()(),
          Html.text("Third child")
        )

        val element = divNode.asInstanceOf[VElement]
        element.children.length shouldBe 3
        element.children(0) shouldBe VText("First child")
        element.children(1) shouldBe a[VElement]
        element.children(2) shouldBe VText("Third child")
      }
    }

    describe("input") {
      it("should create an input element with attributes") {
        val inputNode =
          Html.input("type" -> "text", "placeholder" -> "Enter text")

        val element = inputNode.asInstanceOf[VElement]
        element.tag shouldBe "input"
        element.attrs shouldBe Map(
          "type" -> "text",
          "placeholder" -> "Enter text"
        )
        element.children shouldBe List.empty
      }

      it("should create an input element with events") {
        val inputHandler = IO.unit
        val events = Map("input" -> inputHandler)
        val attrs = Map("type" -> "text")

        val inputNode = Html.input(attrs, events)

        val element = inputNode.asInstanceOf[VElement]
        element.tag shouldBe "input"
        element.attrs shouldBe attrs
        element.events shouldBe events
        element.children shouldBe List.empty
      }
    }

    describe("button") {
      it("should create a button element with text content") {
        val buttonNode = Html.button("type" -> "submit")(
          Html.text("Submit")
        )

        val element = buttonNode.asInstanceOf[VElement]
        element.tag shouldBe "button"
        element.attrs shouldBe Map("type" -> "submit")
        element.children.length shouldBe 1
        element.children.head shouldBe VText("Submit")
      }

      it("should create a button element with events") {
        val clickHandler = IO.unit
        val events = Map("click" -> clickHandler)
        val attrs = Map("type" -> "button")

        val buttonNode = Html.button(attrs, events)(Html.text("Click"))

        val element = buttonNode.asInstanceOf[VElement]
        element.tag shouldBe "button"
        element.attrs shouldBe attrs
        element.events shouldBe events
      }
    }

    describe("text") {
      it("should create a text node") {
        val textNode = Html.text("Hello World")
        textNode shouldBe VText("Hello World")
      }

      it("should handle empty text") {
        val textNode = Html.text("")
        textNode shouldBe VText("")
      }
    }

    describe("other elements") {
      it("should create span elements") {
        val spanNode = Html.span("class" -> "highlight")(Html.text("Important"))

        val element = spanNode.asInstanceOf[VElement]
        element.tag shouldBe "span"
        element.attrs shouldBe Map("class" -> "highlight")
      }

      it("should create ul and li elements") {
        val listNode = Html.ul("class" -> "items")(
          Html.li()(Html.text("Item 1")),
          Html.li()(Html.text("Item 2"))
        )

        val element = listNode.asInstanceOf[VElement]
        element.tag shouldBe "ul"
        element.children.length shouldBe 2
        element.children.foreach(_ shouldBe a[VElement])
      }

      it("should create label elements") {
        val labelNode = Html.label("for" -> "input1")(Html.text("Label text"))

        val element = labelNode.asInstanceOf[VElement]
        element.tag shouldBe "label"
        element.attrs shouldBe Map("for" -> "input1")
      }

      it("should create header, section, and footer elements") {
        val headerNode = Html.header()(Html.h1()(Html.text("Title")))
        val sectionNode = Html.section()(Html.text("Content"))
        val footerNode = Html.footer()(Html.text("Footer"))

        headerNode.asInstanceOf[VElement].tag shouldBe "header"
        sectionNode.asInstanceOf[VElement].tag shouldBe "section"
        footerNode.asInstanceOf[VElement].tag shouldBe "footer"
      }
    }

    describe("nested structures") {
      it("should create complex nested structures") {
        val complexNode = Html.div("class" -> "container")(
          Html.header()(
            Html.h1()(Html.text("My App"))
          ),
          Html.section("class" -> "main")(
            Html.ul()(
              Html.li()(Html.text("Item 1")),
              Html.li()(Html.text("Item 2"))
            )
          ),
          Html.footer()(
            Html.text("© 2024")
          )
        )

        val element = complexNode.asInstanceOf[VElement]
        element.tag shouldBe "div"
        element.children.length shouldBe 3

        // Verify header
        val header = element.children(0).asInstanceOf[VElement]
        header.tag shouldBe "header"

        // Verify section with list
        val section = element.children(1).asInstanceOf[VElement]
        section.tag shouldBe "section"
        section.attrs shouldBe Map("class" -> "main")

        // Verify footer
        val footer = element.children(2).asInstanceOf[VElement]
        footer.tag shouldBe "footer"
      }
    }
  }
}
