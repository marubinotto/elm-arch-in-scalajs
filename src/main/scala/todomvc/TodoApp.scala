package todomvc

import architecture._
import cats.effect.IO
import com.softwaremill.quicklens._
import vdom.VNode

/** TodoMVC application implementing the Elm Architecture pattern
  */
object TodoApp extends App[TodoModel, TodoMsg] {

  /** Initialize the application with an empty model and load command
    *
    * @return
    *   Initial model and command to load todos from storage
    */
  def init: (TodoModel, Cmd[TodoMsg]) = {
    val initialModel = TodoModel.init
    (initialModel, Cmd.task(loadTodosFromStorage))
  }

  /** Update the model based on a message
    *
    * @param msg
    *   The message to process
    * @param model
    *   The current model state
    * @return
    *   Updated model and any commands to execute
    */
  def update(msg: TodoMsg, model: TodoModel): Update[TodoModel, TodoMsg] =
    msg match {

      // Add a new todo with input validation
      case AddTodo(text) =>
        val trimmedText = text.trim
        if (trimmedText.nonEmpty) {
          val newTodo = Todo.create(model.nextId, trimmedText)
          val updatedModel = model
            .modify(_.todos)
            .using(_ :+ newTodo)
            .modify(_.newTodoText)
            .setTo("")
          Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))
        } else {
          // Invalid input - no change to model, no command
          Update(model, Cmd.none)
        }

      // Toggle the completed status of a todo
      case ToggleTodo(id) =>
        val updatedModel = model
          .modify(_.todos.eachWhere(_.id == id).completed)
          .using(!_)
        Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))

      // Update the new todo input text in real-time
      case UpdateNewTodoText(text) =>
        val updatedModel = model.modify(_.newTodoText).setTo(text)
        Update(updatedModel, Cmd.none)

      // Set the current filter for todo visibility
      case SetFilter(filter) =>
        val updatedModel = model.modify(_.filter).setTo(filter)
        Update(updatedModel, Cmd.none)

      // Load todos from storage (typically on app initialization)
      case LoadTodos(todos) =>
        val updatedModel = model.modify(_.todos).setTo(todos)
        Update(updatedModel, Cmd.none)

      // Handle save completion (no model change needed)
      case SaveComplete =>
        Update(model, Cmd.none)

      // Delete a todo by ID
      case DeleteTodo(id) =>
        val updatedModel = model
          .modify(_.todos)
          .using(_.filterNot(_.id == id))
        Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))

      // Enter edit mode for a specific todo
      case EditTodo(id) =>
        model.getTodo(id) match {
          case Some(todo) =>
            val updatedModel = model
              .modify(_.editingTodo)
              .setTo(Some(id))
              .modify(_.editText)
              .setTo(todo.text)
              .modify(_.todos.eachWhere(_.id == id).editing)
              .setTo(true)
            Update(updatedModel, Cmd.none)
          case None =>
            // Todo not found - no change
            Update(model, Cmd.none)
        }

      // Update a todo's text with validation
      case UpdateTodo(id, text) =>
        val trimmedText = text.trim
        if (trimmedText.nonEmpty) {
          val updatedModel = model
            .modify(_.todos.eachWhere(_.id == id))
            .using(_.copy(text = trimmedText, editing = false))
            .modify(_.editingTodo)
            .setTo(None)
            .modify(_.editText)
            .setTo("")
          Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))
        } else {
          // Empty text - delete the todo instead
          val updatedModel = model
            .modify(_.todos)
            .using(_.filterNot(_.id == id))
            .modify(_.editingTodo)
            .setTo(None)
            .modify(_.editText)
            .setTo("")
          Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))
        }

      // Cancel editing without saving changes
      case CancelEdit =>
        val updatedModel = model
          .modify(_.todos.each.editing)
          .setTo(false)
          .modify(_.editingTodo)
          .setTo(None)
          .modify(_.editText)
          .setTo("")
        Update(updatedModel, Cmd.none)

      // Update the edit input text in real-time
      case UpdateEditText(text) =>
        val updatedModel = model.modify(_.editText).setTo(text)
        Update(updatedModel, Cmd.none)

      // Toggle all todos between completed and not completed
      case ToggleAll =>
        val shouldComplete = !model.allCompleted
        val updatedModel = model
          .modify(_.todos.each.completed)
          .setTo(shouldComplete)
        Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))

      // Remove all completed todos
      case ClearCompleted =>
        val updatedModel = model
          .modify(_.todos)
          .using(_.filterNot(_.completed))
        Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))

      // Trigger auto-save operation
      case AutoSave =>
        Update(model, Cmd.task(saveTodosToStorage(model.todos)))

      // Handle network or storage errors
      case NetworkError(error) =>
        // For now, just log the error and continue
        // In a real app, you might show user feedback
        Update(model, Cmd.none)

      // Handle validation errors
      case ValidationError(field, message) =>
        // For now, just log the error and continue
        // In a real app, you might show user feedback
        Update(model, Cmd.none)
    }

  /** Render the current model as a virtual DOM tree
    *
    * @param model
    *   The current model state
    * @return
    *   Virtual DOM representation of the UI
    */
  def view(model: TodoModel): VNode = {
    // Placeholder implementation - will be implemented in task 10
    vdom.VText("TodoMVC App - View not yet implemented")
  }

  /** Define subscriptions for external events
    *
    * @param model
    *   The current model state
    * @return
    *   Subscriptions for this model state
    */
  override def subscriptions(model: TodoModel): Sub[TodoMsg] = {
    // Auto-save every 30 seconds if there are todos
    if (model.todos.nonEmpty) {
      Sub.interval(
        scala.concurrent.duration.FiniteDuration(30, "seconds"),
        AutoSave
      )
    } else {
      Sub.none
    }
  }

  // Private helper functions for side effects

  /** Load todos from local storage
    *
    * @return
    *   IO operation that loads todos and returns a LoadTodos message
    */
  private def loadTodosFromStorage: IO[TodoMsg] = {
    // Placeholder implementation - will be implemented in task 11
    IO.pure(LoadTodos(List.empty))
  }

  /** Save todos to local storage
    *
    * @param todos
    *   The todos to save
    * @return
    *   IO operation that saves todos and returns a SaveComplete message
    */
  private def saveTodosToStorage(todos: List[Todo]): IO[TodoMsg] = {
    // Placeholder implementation - will be implemented in task 11
    IO.pure(SaveComplete)
  }
}
