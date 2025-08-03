package todomvc

import architecture.ValidationError
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TodoValidationSpec extends AnyFunSpec with Matchers {

  describe("TodoValidation") {
    describe("validateTodoText") {
      it("should accept valid text") {
        val result = TodoValidation.validateTodoText("Valid todo text")
        result shouldBe Right("Valid todo text")
      }

      it("should trim whitespace") {
        val result = TodoValidation.validateTodoText("  Valid todo text  ")
        result shouldBe Right("Valid todo text")
      }

      it("should reject empty text") {
        val result = TodoValidation.validateTodoText("")
        result.isLeft shouldBe true
        result.left.get.field shouldBe "text"
      }

      it("should reject whitespace-only text") {
        val result = TodoValidation.validateTodoText("   ")
        result.isLeft shouldBe true
        result.left.get.field shouldBe "text"
      }

      it("should reject text that's too long") {
        val longText = "a" * 1001
        val result = TodoValidation.validateTodoText(longText)
        result.isLeft shouldBe true
        result.left.get.field shouldBe "text"
        result.left.get.message should include("too long")
      }

      it("should reject text with line breaks") {
        val result = TodoValidation.validateTodoText("Text with\nline break")
        result.isLeft shouldBe true
        result.left.get.field shouldBe "text"
        result.left.get.message should include("line breaks")
      }
    }

    describe("validateTodoId") {
      it("should accept positive IDs") {
        val result = TodoValidation.validateTodoId(1)
        result shouldBe Right(1)
      }

      it("should reject zero ID") {
        val result = TodoValidation.validateTodoId(0)
        result.isLeft shouldBe true
        result.left.get.field shouldBe "id"
      }

      it("should reject negative IDs") {
        val result = TodoValidation.validateTodoId(-1)
        result.isLeft shouldBe true
        result.left.get.field shouldBe "id"
      }
    }

    describe("validateTodoExists") {
      val model = TodoModel.withTodos(
        List(
          Todo(1, "Todo 1", completed = false, editing = false),
          Todo(2, "Todo 2", completed = true, editing = false)
        )
      )

      it("should find existing todo") {
        val result = TodoValidation.validateTodoExists(1, model)
        result.isRight shouldBe true
        result.right.get.id shouldBe 1
      }

      it("should reject non-existent todo") {
        val result = TodoValidation.validateTodoExists(999, model)
        result.isLeft shouldBe true
        result.left.get.field shouldBe "id"
      }
    }

    describe("validateTodoList") {
      it("should accept valid todo list") {
        val todos = List(
          Todo(1, "Todo 1", completed = false, editing = false),
          Todo(2, "Todo 2", completed = true, editing = false)
        )
        val result = TodoValidation.validateTodoList(todos)
        result shouldBe Right(todos)
      }

      it("should accept empty list") {
        val result = TodoValidation.validateTodoList(List.empty)
        result shouldBe Right(List.empty)
      }

      it("should reject list with duplicate IDs") {
        val todos = List(
          Todo(1, "Todo 1", completed = false, editing = false),
          Todo(1, "Todo 2", completed = true, editing = false)
        )
        val result = TodoValidation.validateTodoList(todos)
        result.isLeft shouldBe true
        result.left.get.field shouldBe "todos"
        result.left.get.message should include("Duplicate")
      }

      it("should reject list with invalid IDs") {
        val todos = List(
          Todo(0, "Todo 1", completed = false, editing = false),
          Todo(2, "Todo 2", completed = true, editing = false)
        )
        val result = TodoValidation.validateTodoList(todos)
        result.isLeft shouldBe true
        result.left.get.field shouldBe "todos"
        result.left.get.message should include("positive")
      }

      it("should reject list that's too large") {
        val todos = (1 to 10001)
          .map(i => Todo(i, s"Todo $i", completed = false, editing = false))
          .toList
        val result = TodoValidation.validateTodoList(todos)
        result.isLeft shouldBe true
        result.left.get.field shouldBe "todos"
        result.left.get.message should include("Too many")
      }
    }

    describe("validateModel") {
      it("should accept valid model") {
        val model = TodoModel.withTodos(
          List(
            Todo(1, "Todo 1", completed = false, editing = false),
            Todo(2, "Todo 2", completed = true, editing = false)
          )
        )
        val result = TodoValidation.validateModel(model)
        result shouldBe Right(model)
      }

      it("should reject model with invalid todos") {
        val model = TodoModel.withTodos(
          List(
            Todo(1, "Todo 1", completed = false, editing = false),
            Todo(1, "Todo 2", completed = true, editing = false) // Duplicate ID
          )
        )
        val result = TodoValidation.validateModel(model)
        result.isLeft shouldBe true
      }

      it("should reject model with invalid editing todo") {
        val model = TodoModel
          .withTodos(
            List(
              Todo(1, "Todo 1", completed = false, editing = false)
            )
          )
          .copy(editingTodo = Some(999)) // Non-existent ID
        val result = TodoValidation.validateModel(model)
        result.isLeft shouldBe true
      }
    }

    describe("safeUpdateModel") {
      val validModel = TodoModel.withTodos(
        List(
          Todo(1, "Todo 1", completed = false, editing = false)
        )
      )

      it("should apply valid updates") {
        val update = (model: TodoModel) => model.copy(newTodoText = "Updated")
        val result =
          TodoValidation.safeUpdateModel(validModel, update, validModel)
        result.newTodoText.shouldBe("Updated")
      }

      it("should use fallback on invalid update") {
        val invalidUpdate = (model: TodoModel) =>
          model.copy(todos =
            List(
              Todo(1, "Todo 1", completed = false, editing = false),
              Todo(
                1,
                "Todo 2",
                completed = true,
                editing = false
              ) // Duplicate ID
            )
          )
        val result =
          TodoValidation.safeUpdateModel(validModel, invalidUpdate, validModel)
        result shouldBe validModel
      }

      it("should use fallback on exception") {
        val throwingUpdate =
          (_: TodoModel) => throw new RuntimeException("Update failed")
        val result =
          TodoValidation.safeUpdateModel(validModel, throwingUpdate, validModel)
        result shouldBe validModel
      }
    }

    describe("sanitizeInput") {
      it("should handle null input") {
        val result = TodoValidation.sanitizeInput(null)
        result shouldBe ""
      }

      it("should trim whitespace") {
        val result = TodoValidation.sanitizeInput("  text  ")
        result shouldBe "text"
      }

      it("should normalize multiple spaces") {
        val result = TodoValidation.sanitizeInput("text   with    spaces")
        result shouldBe "text with spaces"
      }

      it("should remove control characters") {
        val result = TodoValidation.sanitizeInput("text\u0000with\u001Fcontrol")
        result shouldBe "textwithcontrol"
      }
    }

    describe("isModelConsistent") {
      it("should return true for valid model") {
        val model = TodoModel.withTodos(
          List(
            Todo(1, "Todo 1", completed = false, editing = false)
          )
        )
        TodoValidation.isModelConsistent(model) shouldBe true
      }

      it("should return false for invalid model") {
        val model = TodoModel.withTodos(
          List(
            Todo(1, "Todo 1", completed = false, editing = false),
            Todo(1, "Todo 2", completed = true, editing = false) // Duplicate ID
          )
        )
        TodoValidation.isModelConsistent(model) shouldBe false
      }
    }

    describe("getModelErrors") {
      it("should return empty list for valid model") {
        val model = TodoModel.withTodos(
          List(
            Todo(1, "Todo 1", completed = false, editing = false)
          )
        )
        val errors = TodoValidation.getModelErrors(model)
        errors shouldBe empty
      }

      it("should return errors for invalid model") {
        val model = TodoModel.withTodos(
          List(
            Todo(1, "Todo 1", completed = false, editing = false),
            Todo(1, "Todo 2", completed = true, editing = false) // Duplicate ID
          )
        )
        val errors = TodoValidation.getModelErrors(model)
        errors should not be empty
      }
    }
  }
}
