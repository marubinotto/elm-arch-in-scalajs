package todomvc

/** Todo item data model
  *
  * @param id
  *   Unique identifier for the todo item (using Int for compatibility)
  * @param text
  *   The text content of the todo
  * @param completed
  *   Whether the todo is marked as completed
  * @param editing
  *   Whether the todo is currently being edited
  */
case class Todo(
    id: Int,
    text: String,
    completed: Boolean,
    editing: Boolean
)

object Todo {

  /** Create a new todo with default values
    *
    * @param id
    *   Unique identifier
    * @param text
    *   Todo text content (will be sanitized)
    * @return
    *   New todo item that is not completed and not being edited
    */
  def create(id: Int, text: String): Todo = {
    val sanitizedText = Option(text).getOrElse("").trim
    require(sanitizedText.nonEmpty, "Todo text cannot be empty")
    require(id > 0, "Todo ID must be positive")

    Todo(
      id = id,
      text = sanitizedText,
      completed = false,
      editing = false
    )
  }

  /** Update todo text with sanitization */
  def updateText(todo: Todo, newText: String): Todo = {
    val sanitizedText = Option(newText).getOrElse("").trim
    require(sanitizedText.nonEmpty, "Todo text cannot be empty")
    todo.copy(text = sanitizedText)
  }
}
