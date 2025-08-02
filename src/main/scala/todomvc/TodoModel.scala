package todomvc

/** Application state model for TodoMVC
  *
  * @param todos
  *   List of all todo items
  * @param newTodoText
  *   Current text in the new todo input field
  * @param filter
  *   Current filter for displaying todos
  * @param editingTodo
  *   ID of the todo currently being edited (if any)
  * @param editText
  *   Text content during editing
  */
case class TodoModel(
    todos: List[Todo],
    newTodoText: String,
    filter: TodoFilter,
    editingTodo: Option[Int],
    editText: String
) {

  /** Get todos that match the current filter */
  def filteredTodos: List[Todo] = todos.filter(filter.matches)

  /** Get the count of active (not completed) todos */
  def activeCount: Int = todos.count(!_.completed)

  /** Get the count of completed todos */
  def completedCount: Int = todos.count(_.completed)

  /** Check if all todos are completed */
  def allCompleted: Boolean = todos.nonEmpty && todos.forall(_.completed)

  /** Check if there are any completed todos */
  def hasCompleted: Boolean = todos.exists(_.completed)

  /** Generate the next available todo ID */
  def nextId: Int = if (todos.isEmpty) 1 else todos.map(_.id).max + 1

  /** Get a todo by ID */
  def getTodo(id: Int): Option[Todo] = todos.find(_.id == id)

  /** Check if a todo is currently being edited */
  def isEditing(id: Int): Boolean = editingTodo.contains(id)
}

object TodoModel {

  /** Create an initial empty TodoModel */
  def init: TodoModel = TodoModel(
    todos = List.empty,
    newTodoText = "",
    filter = All,
    editingTodo = None,
    editText = ""
  )

  /** Create a TodoModel with initial todos */
  def withTodos(todos: List[Todo]): TodoModel = init.copy(todos = todos)
}
