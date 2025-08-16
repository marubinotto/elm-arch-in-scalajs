package todomvc

/** Opaque type for Todo IDs to prevent invalid ID usage */
opaque type TodoId = Int

object TodoId {
  def apply(value: Int): TodoId = {
    require(value > 0, "TodoId must be positive")
    value
  }

  def unsafe(value: Int): TodoId = value

  extension (id: TodoId) {
    def value: Int = id
  }
}

/** Application state model for TodoMVC
  *
  * Uses type-driven design to prevent invalid states:
  *   - Map prevents duplicate todo IDs by construction
  *   - Direct todo reference for editing prevents dangling references
  *   - Opaque TodoId type prevents invalid ID usage
  *
  * @param todos
  *   Map of all todo items (prevents duplicates by construction)
  * @param newTodoText
  *   Current text in the new todo input field
  * @param filter
  *   Current filter for displaying todos
  * @param editingTodo
  *   Direct reference to todo being edited (guarantees existence)
  * @param nextId
  *   Next available ID for new todos
  */
case class TodoModel(
    todos: Map[TodoId, Todo],
    newTodoText: String,
    filter: TodoFilter,
    editingTodo: Option[Todo],
    nextId: TodoId
) {

  /** Get todos that match the current filter */
  def filteredTodos: List[Todo] = todos.values.filter(filter.matches).toList

  /** Get the count of active (not completed) todos */
  def activeCount: Int = todos.values.count(!_.completed)

  /** Get the count of completed todos */
  def completedCount: Int = todos.values.count(_.completed)

  /** Check if all todos are completed */
  def allCompleted: Boolean = todos.nonEmpty && todos.values.forall(_.completed)

  /** Check if there are any completed todos */
  def hasCompleted: Boolean = todos.values.exists(_.completed)

  /** Get a todo by ID */
  def getTodo(id: TodoId): Option[Todo] = todos.get(id)

  /** Check if a todo is currently being edited */
  def isEditing(todo: Todo): Boolean = editingTodo.contains(todo)

  /** Get all todos as a list (for compatibility) */
  def todoList: List[Todo] = todos.values.toList
}

object TodoModel {

  /** Create an initial empty TodoModel */
  def init: TodoModel = TodoModel(
    todos = Map.empty,
    newTodoText = "",
    filter = All,
    editingTodo = None,
    nextId = TodoId(1)
  )

  /** Create a TodoModel with initial todos (smart constructor) */
  def withTodos(todos: List[Todo]): TodoModel = {
    val todoMap = todos.map(todo => TodoId.unsafe(todo.id) -> todo).toMap
    val maxId = if (todos.isEmpty) 0 else todos.map(_.id).max
    TodoModel(
      todos = todoMap,
      newTodoText = "",
      filter = All,
      editingTodo = None,
      nextId = TodoId(maxId + 1)
    )
  }

  /** Add a todo to the model */
  def addTodo(model: TodoModel, text: String): TodoModel = {
    val sanitizedText = text.trim
    if (sanitizedText.isEmpty) {
      model // Don't add empty todos
    } else {
      val newTodo = Todo.create(model.nextId.value, sanitizedText)
      model.copy(
        todos = model.todos + (model.nextId -> newTodo),
        nextId = TodoId(model.nextId.value + 1),
        newTodoText = "" // Clear input
      )
    }
  }

  /** Remove a todo from the model */
  def removeTodo(model: TodoModel, id: TodoId): TodoModel = {
    val updatedTodos = model.todos - id
    val updatedEditing = model.editingTodo.filter(_.id != id.value)
    model.copy(
      todos = updatedTodos,
      editingTodo = updatedEditing
    )
  }

  /** Update a todo in the model */
  def updateTodo(
      model: TodoModel,
      id: TodoId,
      updater: Todo => Todo
  ): TodoModel = {
    model.todos.get(id) match {
      case Some(todo) =>
        val updatedTodo = updater(todo)
        model.copy(
          todos = model.todos + (id -> updatedTodo),
          editingTodo =
            if (model.editingTodo.contains(todo)) Some(updatedTodo)
            else model.editingTodo
        )
      case None => model // Todo doesn't exist, no change
    }
  }

  /** Start editing a todo */
  def startEditing(model: TodoModel, id: TodoId): TodoModel = {
    model.todos.get(id) match {
      case Some(todo) =>
        val editingTodo = todo.copy(editing = true)
        model.copy(
          todos = model.todos + (id -> editingTodo),
          editingTodo = Some(editingTodo)
        )
      case None => model // Todo doesn't exist, no change
    }
  }

  /** Stop editing (cancel or save) */
  def stopEditing(model: TodoModel): TodoModel = {
    model.editingTodo match {
      case Some(editingTodo) =>
        val updatedTodo = editingTodo.copy(editing = false)
        model.copy(
          todos = model.todos + (TodoId.unsafe(editingTodo.id) -> updatedTodo),
          editingTodo = None
        )
      case None => model
    }
  }
}
