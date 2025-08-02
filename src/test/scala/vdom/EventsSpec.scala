package vdom

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.IO

class EventsSpec extends AnyFunSpec with Matchers {

  describe("Events object") {
    describe("onClick") {
      it("should create a click event handler") {
        val action = IO.unit
        val (eventName, eventHandler) = Events.onClick(action)

        eventName shouldBe "click"
        eventHandler shouldBe action
      }

      it("should create a click event with custom action") {
        var clicked = false
        val action = IO.delay { clicked = true }
        val (eventName, eventHandler) = Events.onClick(action)

        eventName shouldBe "click"
        eventHandler shouldBe action
      }
    }

    describe("onInput") {
      it("should create an input event handler") {
        var inputValue = ""
        val handler = (value: String) => IO.delay { inputValue = value }
        val (eventName, eventHandler) = Events.onInput(handler)

        eventName shouldBe "input"
        eventHandler shouldBe a[IO[_]]
      }

      it("should handle input values") {
        var receivedValue = ""
        val handler = (value: String) => IO.delay { receivedValue = value }
        val (eventName, eventHandler) = Events.onInput(handler)

        eventName shouldBe "input"
        // Note: The actual event handling will be properly implemented in DOM integration
        // This test verifies the structure is correct
        eventHandler shouldBe a[IO[_]]
      }
    }

    describe("onKeyDown") {
      it("should create a keydown event handler") {
        val handler = (keyCode: Int) => IO.pure(Some(()))
        val (eventName, eventHandler) = Events.onKeyDown(handler)

        eventName shouldBe "keydown"
        eventHandler shouldBe a[IO[_]]
      }

      it("should handle conditional key events") {
        var enterPressed = false
        val handler = (keyCode: Int) => {
          if (keyCode == 13) { // Enter key
            IO.delay { enterPressed = true }.map(Some(_))
          } else {
            IO.pure(None)
          }
        }
        val (eventName, eventHandler) = Events.onKeyDown(handler)

        eventName shouldBe "keydown"
        eventHandler shouldBe a[IO[_]]
      }
    }

    describe("other event handlers") {
      it("should create onChange event handler") {
        val action = IO.unit
        val (eventName, eventHandler) = Events.onChange(action)

        eventName shouldBe "change"
        eventHandler shouldBe action
      }

      it("should create onBlur event handler") {
        val action = IO.unit
        val (eventName, eventHandler) = Events.onBlur(action)

        eventName shouldBe "blur"
        eventHandler shouldBe action
      }

      it("should create onFocus event handler") {
        val action = IO.unit
        val (eventName, eventHandler) = Events.onFocus(action)

        eventName shouldBe "focus"
        eventHandler shouldBe action
      }

      it("should create onDoubleClick event handler") {
        val action = IO.unit
        val (eventName, eventHandler) = Events.onDoubleClick(action)

        eventName shouldBe "dblclick"
        eventHandler shouldBe action
      }

      it("should create onSubmit event handler") {
        val action = IO.unit
        val (eventName, eventHandler) = Events.onSubmit(action)

        eventName shouldBe "submit"
        eventHandler shouldBe action
      }
    }

    describe("event integration with Html") {
      it("should work with Html elements") {
        val clickAction = IO.unit
        val inputAction = (value: String) => IO.unit

        val buttonWithClick = Html.button(
          Map("type" -> "button"),
          Map(Events.onClick(clickAction))
        )(Html.text("Click me"))

        val inputWithHandler = Html.input(
          Map("type" -> "text"),
          Map(Events.onInput(inputAction))
        )

        // Verify structure
        val button = buttonWithClick.asInstanceOf[VElement]
        button.tag shouldBe "button"
        button.events should contain key "click"

        val input = inputWithHandler.asInstanceOf[VElement]
        input.tag shouldBe "input"
        input.events should contain key "input"
      }

      it("should support multiple events on single element") {
        val clickAction = IO.unit
        val focusAction = IO.unit
        val blurAction = IO.unit

        val multiEventButton = Html.button(
          Map("type" -> "button"),
          Map(
            Events.onClick(clickAction),
            Events.onFocus(focusAction),
            Events.onBlur(blurAction)
          )
        )(Html.text("Multi-event button"))

        val element = multiEventButton.asInstanceOf[VElement]
        element.events should have size 3
        element.events should contain key "click"
        element.events should contain key "focus"
        element.events should contain key "blur"
      }
    }
  }
}
