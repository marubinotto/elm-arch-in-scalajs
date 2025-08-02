package todomvc

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class TodoMsgSpec extends AnyFreeSpec with Matchers {

  "TodoMsg" - {

    "should create AddTodo messages correctly" in {
      TodoMsg.addTodo("Valid todo") shouldBe Some(AddTodo("Valid todo"))
      TodoMsg.addTodo("  Trimmed todo  ") shouldBe Some(AddTodo("Trimmed todo"))
      TodoMsg.addTodo("") shouldBe None
      TodoMsg.addTodo("   ") shouldBe None
    }

    "should create UpdateTodo messages correctly" in {
      TodoMsg.updateTodo(1, "Valid update") shouldBe Some(
        UpdateTodo(1, "Valid update")
      )
      TodoMsg.updateTodo(1, "  Trimmed update  ") shouldBe Some(
        UpdateTodo(1, "Trimmed update")
      )
      TodoMsg.updateTodo(1, "") shouldBe None
      TodoMsg.updateTodo(1, "   ") shouldBe None
    }
  }

  "Message types" - {

    "should be properly typed" in {
      val addMsg: TodoMsg = AddTodo("test")
      val toggleMsg: TodoMsg = ToggleTodo(1)
      val deleteMsg: TodoMsg = DeleteTodo(1)
      val editMsg: TodoMsg = EditTodo(1)
      val updateMsg: TodoMsg = UpdateTodo(1, "updated")
      val cancelMsg: TodoMsg = CancelEdit
      val newTextMsg: TodoMsg = UpdateNewTodoText("new text")
      val editTextMsg: TodoMsg = UpdateEditText("edit text")
      val filterMsg: TodoMsg = SetFilter(Active)
      val toggleAllMsg: TodoMsg = ToggleAll
      val clearMsg: TodoMsg = ClearCompleted
      val loadMsg: TodoMsg = LoadTodos(List.empty)
      val saveMsg: TodoMsg = SaveComplete
      val autoSaveMsg: TodoMsg = AutoSave
      val networkErrorMsg: TodoMsg = NetworkError("error")
      val validationErrorMsg: TodoMsg = ValidationError("field", "message")

      // All messages should be of type TodoMsg
      List(
        addMsg,
        toggleMsg,
        deleteMsg,
        editMsg,
        updateMsg,
        cancelMsg,
        newTextMsg,
        editTextMsg,
        filterMsg,
        toggleAllMsg,
        clearMsg,
        loadMsg,
        saveMsg,
        autoSaveMsg,
        networkErrorMsg,
        validationErrorMsg
      ).foreach(_ shouldBe a[TodoMsg])
    }

    "should support pattern matching" in {
      val msg: TodoMsg = AddTodo("test")

      val result = msg match {
        case AddTodo(text)  => s"Adding: $text"
        case ToggleTodo(id) => s"Toggling: $id"
        case _              => "Other message"
      }

      result shouldBe "Adding: test"
    }
  }
}
