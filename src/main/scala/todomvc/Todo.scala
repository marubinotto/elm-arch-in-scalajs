package todomvc

/** Todo item data model
  *
  * @param id
  *   Unique identifier for the todo item
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
    *   Todo text content
    * @return
    *   New todo item that is not completed and not being edited
    */
  def create(id: Int, text: String): Todo = Todo(
    id = id,
    text = text,
    completed = false,
    editing = false
  )
}
