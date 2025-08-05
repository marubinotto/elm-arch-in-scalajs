package todomvc

/** All possible messages that can be sent to update the TodoMVC application
  * state
  */
sealed trait TodoMsg

// User action messages for todo management
/** Add a new todo with the given text */
case class AddTodo(text: String) extends TodoMsg

/** Toggle the completed status of a todo by ID */
case class ToggleTodo(id: Int) extends TodoMsg

/** Delete a todo by ID */
case class DeleteTodo(id: Int) extends TodoMsg

/** Enter edit mode for a todo by ID */
case class EditTodo(id: Int) extends TodoMsg

/** Update the text of a todo by ID */
case class UpdateTodo(id: Int, text: String) extends TodoMsg

/** Update todo text only if still editing (for blur events) */
case class UpdateTodoIfEditing(id: Int, text: String) extends TodoMsg

/** Cancel editing mode without saving changes */
case object CancelEdit extends TodoMsg

// Input field messages
/** Update the text in the new todo input field */
case class UpdateNewTodoText(text: String) extends TodoMsg

/** Update the text in the edit input field */
case class UpdateEditText(text: String) extends TodoMsg

// Filter messages
/** Set the current filter for displaying todos */
case class SetFilter(filter: TodoFilter) extends TodoMsg

// Bulk action messages
/** Toggle all todos between completed and not completed */
case object ToggleAll extends TodoMsg

/** Clear all completed todos */
case object ClearCompleted extends TodoMsg

// Side effect messages for persistence and external events
/** Load todos from storage (typically on app initialization) */
case class LoadTodos(todos: List[Todo]) extends TodoMsg

/** Indicates that a save operation has completed successfully */
case object SaveComplete extends TodoMsg

/** Trigger an auto-save operation */
case object AutoSave extends TodoMsg

object TodoMsg {

  /** Helper to create an AddTodo message with trimmed text */
  def addTodo(text: String): Option[AddTodo] = {
    val trimmed = text.trim
    if (trimmed.nonEmpty) Some(AddTodo(trimmed)) else None
  }

  /** Helper to create an UpdateTodo message with trimmed text */
  def updateTodo(id: Int, text: String): Option[UpdateTodo] = {
    val trimmed = text.trim
    if (trimmed.nonEmpty) Some(UpdateTodo(id, trimmed)) else None
  }
}
