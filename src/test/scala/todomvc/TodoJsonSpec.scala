package todomvc

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Success, Failure}

/** Unit tests for TodoJson serialization and deserialization
  */
class TodoJsonSpec extends AnyFunSpec with Matchers {

  describe("TodoJson") {

    describe("serializeTodo and parseTodo") {
      it("should serialize and deserialize a single todo") {
        val todo = Todo(1, "Buy milk", completed = false, editing = false)

        val json = TodoJson.serializeTodo(todo)
        val parsed = TodoJson.parseTodo(json)

        parsed should be(Success(todo))
      }

      it("should handle completed todo") {
        val todo = Todo(2, "Walk dog", completed = true, editing = false)

        val json = TodoJson.serializeTodo(todo)
        val parsed = TodoJson.parseTodo(json)

        parsed should be(Success(todo))
      }

      it("should handle editing todo") {
        val todo = Todo(3, "Write code", completed = false, editing = true)

        val json = TodoJson.serializeTodo(todo)
        val parsed = TodoJson.parseTodo(json)

        parsed should be(Success(todo))
      }

      it("should handle special characters in text") {
        val todo = Todo(
          4,
          "Test with \"quotes\" and 'apostrophes' and unicode: 🚀",
          completed = false,
          editing = false
        )

        val json = TodoJson.serializeTodo(todo)
        val parsed = TodoJson.parseTodo(json)

        parsed should be(Success(todo))
      }

      it("should handle newlines and tabs in text") {
        val todo = Todo(
          5,
          "Test with\nnewlines\tand\ttabs",
          completed = false,
          editing = false
        )

        val json = TodoJson.serializeTodo(todo)
        val parsed = TodoJson.parseTodo(json)

        parsed should be(Success(todo))
      }

      it("should fail gracefully with invalid JSON") {
        val invalidJson = "invalid json"

        val parsed = TodoJson.parseTodo(invalidJson)

        parsed shouldBe a[Failure[_]]
      }
    }

    describe("serializeTodos and parseTodos") {
      it("should serialize and deserialize empty todo list") {
        val todos = List.empty[Todo]

        val json = TodoJson.serializeTodos(todos)
        val parsed = TodoJson.parseTodos(json)

        parsed should be(Success(todos))
      }

      it("should serialize and deserialize single todo list") {
        val todos =
          List(Todo(1, "Single todo", completed = false, editing = false))

        val json = TodoJson.serializeTodos(todos)
        val parsed = TodoJson.parseTodos(json)

        parsed should be(Success(todos))
      }

      it("should serialize and deserialize multiple todos") {
        val todos = List(
          Todo(1, "Buy milk", completed = false, editing = false),
          Todo(2, "Walk dog", completed = true, editing = false),
          Todo(3, "Write code", completed = false, editing = true),
          Todo(4, "Review PR", completed = true, editing = false)
        )

        val json = TodoJson.serializeTodos(todos)
        val parsed = TodoJson.parseTodos(json)

        parsed should be(Success(todos))
      }

      it("should preserve todo order") {
        val todos = List(
          Todo(10, "Last created", completed = false, editing = false),
          Todo(1, "First created", completed = true, editing = false),
          Todo(5, "Middle created", completed = false, editing = true)
        )

        val json = TodoJson.serializeTodos(todos)
        val parsed = TodoJson.parseTodos(json)

        parsed should be(Success(todos))
        parsed.get should equal(todos)
      }

      it("should handle todos with duplicate IDs") {
        val todos = List(
          Todo(1, "First todo", completed = false, editing = false),
          Todo(1, "Duplicate ID todo", completed = true, editing = false)
        )

        val json = TodoJson.serializeTodos(todos)
        val parsed = TodoJson.parseTodos(json)

        parsed should be(Success(todos))
      }

      it("should fail gracefully with invalid JSON") {
        val invalidJson = "not json"

        val parsed = TodoJson.parseTodos(invalidJson)

        parsed shouldBe a[Failure[_]]
      }

      it("should fail gracefully with non-array JSON") {
        val nonArrayJson = """{"not": "an array"}"""

        val parsed = TodoJson.parseTodos(nonArrayJson)

        parsed shouldBe a[Failure[_]]
      }

      it("should handle malformed todo objects gracefully") {
        val malformedJson = """[{"id": 1, "text": "Missing fields"}]"""

        val parsed = TodoJson.parseTodos(malformedJson)

        // Should succeed with default values for missing fields
        parsed shouldBe a[Success[?]]
        parsed.get should have length 1
        parsed.get.head.completed should be(false)
        parsed.get.head.editing should be(false)
      }
    }

    describe("serializeModel and parseModel") {
      it("should serialize and deserialize TodoModel") {
        val todos = List(
          Todo(1, "Buy milk", completed = false, editing = false),
          Todo(2, "Walk dog", completed = true, editing = false)
        )
        val model = TodoModel
          .withTodos(todos)
          .copy(
            newTodoText = "New todo text",
            filter = Active,
            editingTodo = Some(todos.head)
          )

        val json = TodoJson.serializeModel(model)
        val parsed = TodoJson.parseModel(json)

        parsed should be(Success(model))
      }

      it("should handle model with empty todos") {
        val model = TodoModel.init

        val json = TodoJson.serializeModel(model)
        val parsed = TodoJson.parseModel(json)

        parsed should be(Success(model))
      }

      it("should handle all filter types") {
        val filters = List(All, Active, Completed)

        filters.foreach { filter =>
          val model = TodoModel.init.copy(filter = filter)

          val json = TodoJson.serializeModel(model)
          val parsed = TodoJson.parseModel(json)

          parsed should be(Success(model))
        }
      }

      it("should handle model with no editing todo") {
        val model = TodoModel
          .withTodos(List(Todo(1, "Test", false, false)))
          .copy(
            newTodoText = "Test"
          )

        val json = TodoJson.serializeModel(model)
        val parsed = TodoJson.parseModel(json)

        parsed should be(Success(model))
      }

      it("should fail gracefully with invalid model JSON") {
        val invalidJson = "invalid model json"

        val parsed = TodoJson.parseModel(invalidJson)

        parsed shouldBe a[Failure[_]]
      }

      it("should handle unknown filter gracefully") {
        val jsonWithUnknownFilter =
          """{"todos":[],"newTodoText":"","filter":"Unknown","editingTodo":null,"editText":""}"""

        val parsed = TodoJson.parseModel(jsonWithUnknownFilter)

        parsed shouldBe a[Success[_]]
        parsed.get.filter should be(
          All
        ) // Should default to All for unknown filter
      }
    }

    describe("JSON format validation") {
      it("should produce valid JSON for todos") {
        val todos = List(
          Todo(1, "Test todo", completed = false, editing = false)
        )

        val json = TodoJson.serializeTodos(todos)

        // Should be valid JSON array
        json should startWith("[")
        json should endWith("]")

        // Should contain expected fields
        json should include("\"id\"")
        json should include("\"text\"")
        json should include("\"completed\"")
        json should include("\"editing\"")
      }

      it("should produce valid JSON for model") {
        val model = TodoModel.init

        val json = TodoJson.serializeModel(model)

        // Should be valid JSON object
        json should startWith("{")
        json should endWith("}")

        // Should contain expected fields
        json should include("\"todos\"")
        json should include("\"newTodoText\"")
        json should include("\"filter\"")
        json should include("\"editingTodo\"")
        json should include("\"nextId\"")
      }
    }
  }
}
