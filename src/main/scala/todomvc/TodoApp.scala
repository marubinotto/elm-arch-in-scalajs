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

      // Handle other messages that aren't implemented in this task
      case _ =>
        // For now, return the model unchanged for unhandled messages
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
