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
    import vdom.Html._
    import vdom.Events._

    section("class" -> "todoapp")(
      renderHeader(model),
      renderMain(model),
      renderFooter(model)
    )
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

  // Private helper methods for rendering different sections

  /** Render the header section with new todo input
    *
    * @param model
    *   The current model state
    * @return
    *   Virtual DOM node for the header
    */
  private def renderHeader(model: TodoModel): VNode = {
    import vdom.Html._

    header("class" -> "header")(
      h1()(text("todos")),
      input(
        Map(
          "class" -> "new-todo",
          "placeholder" -> "What needs to be done?",
          "value" -> model.newTodoText,
          "autofocus" -> "true"
        ),
        Map(
          "input" -> onInputText(text =>
            dispatchMessage(UpdateNewTodoText(text))
          ),
          "keydown" -> onEnterKey(dispatchMessage(AddTodo(model.newTodoText)))
        )
      )
    )
  }

  /** Render the main section with todo list
    *
    * @param model
    *   The current model state
    * @return
    *   Virtual DOM node for the main section
    */
  private def renderMain(model: TodoModel): VNode = {
    import vdom.Html._

    if (model.todos.isEmpty) {
      text("") // Empty node when no todos
    } else {
      section("class" -> "main")(
        input(
          Map(
            "id" -> "toggle-all",
            "class" -> "toggle-all",
            "type" -> "checkbox"
          ) ++ (if (model.allCompleted) Map("checked" -> "checked")
                else Map.empty),
          Map(
            "change" -> dispatchMessage(ToggleAll)
          )
        ),
        label("for" -> "toggle-all")(text("Mark all as complete")),
        ul("class" -> "todo-list")(
          model.filteredTodos.map(renderTodoItem(_, model)): _*
        )
      )
    }
  }

  /** Render an individual todo item
    *
    * @param todo
    *   The todo item to render
    * @param model
    *   The current model state
    * @return
    *   Virtual DOM node for the todo item
    */
  private def renderTodoItem(todo: Todo, model: TodoModel): VNode = {
    import vdom.Html._

    val classes = List(
      if (todo.completed) Some("completed") else None,
      if (model.isEditing(todo.id)) Some("editing") else None
    ).flatten.mkString(" ")

    li("class" -> classes)(
      div("class" -> "view")(
        input(
          Map(
            "class" -> "toggle",
            "type" -> "checkbox"
          ) ++ (if (todo.completed) Map("checked" -> "checked") else Map.empty),
          Map(
            "change" -> dispatchMessage(ToggleTodo(todo.id))
          )
        ),
        label(
          Map.empty,
          Map(
            "dblclick" -> dispatchMessage(EditTodo(todo.id))
          )
        )(text(todo.text)),
        button(
          Map("class" -> "destroy"),
          Map(
            "click" -> dispatchMessage(DeleteTodo(todo.id))
          )
        )()
      ),
      if (model.isEditing(todo.id)) {
        input(
          Map(
            "class" -> "edit",
            "value" -> model.editText
          ),
          Map(
            "input" -> onInputText(text =>
              dispatchMessage(UpdateEditText(text))
            ),
            "keydown" -> onEditKeyDown(todo.id, model.editText),
            "blur" -> dispatchMessage(UpdateTodo(todo.id, model.editText))
          )
        )
      } else {
        text("") // Empty node when not editing
      }
    )
  }

  /** Render the footer section with filters and stats
    *
    * @param model
    *   The current model state
    * @return
    *   Virtual DOM node for the footer
    */
  private def renderFooter(model: TodoModel): VNode = {
    import vdom.Html._

    if (model.todos.isEmpty) {
      text("") // Empty node when no todos
    } else {
      footer("class" -> "footer")(
        span("class" -> "todo-count")(
          text(s"${model.activeCount} ${
              if (model.activeCount == 1) "item" else "items"
            } left")
        ),
        ul("class" -> "filters")(
          TodoFilter.all.map(renderFilterButton(_, model.filter)): _*
        ),
        if (model.hasCompleted) {
          button(
            Map("class" -> "clear-completed"),
            Map(
              "click" -> dispatchMessage(ClearCompleted)
            )
          )(text("Clear completed"))
        } else {
          text("") // Empty node when no completed todos
        }
      )
    }
  }

  /** Render a filter button
    *
    * @param filter
    *   The filter option
    * @param currentFilter
    *   The currently selected filter
    * @return
    *   Virtual DOM node for the filter button
    */
  private def renderFilterButton(
      filter: TodoFilter,
      currentFilter: TodoFilter
  ): VNode = {
    import vdom.Html._

    li()(
      a(
        Map(
          "href" -> s"#/${filter.displayName.toLowerCase}"
        ) ++ (if (filter == currentFilter) Map("class" -> "selected")
              else Map.empty),
        Map(
          "click" -> dispatchMessage(SetFilter(filter))
        )
      )(text(filter.displayName))
    )
  }

  // Private helper methods for event handling and message dispatching

  /** Create a message dispatch action
    *
    * @param msg
    *   The message to dispatch
    * @return
    *   IO action that dispatches the message
    */
  private def dispatchMessage(msg: TodoMsg): IO[Unit] = {
    // This will be connected to the runtime's dispatch method
    // For now, this is a placeholder that will be enhanced when integrated with the runtime
    IO.unit
  }

  /** Create an input event handler that extracts text value
    *
    * @param handler
    *   Function to handle the input text
    * @return
    *   IO action for the input event
    */
  private def onInputText(handler: String => IO[Unit]): IO[Unit] = {
    // This will be properly implemented when integrated with the DOM
    // For now, this is a placeholder
    IO.unit
  }

  /** Create an Enter key event handler
    *
    * @param action
    *   Action to perform when Enter is pressed
    * @return
    *   IO action for the keydown event
    */
  private def onEnterKey(action: IO[Unit]): IO[Unit] = {
    // This will be properly implemented when integrated with the DOM
    // For now, this is a placeholder that simulates Enter key (keyCode 13)
    IO.unit
  }

  /** Create a keydown event handler for edit mode
    *
    * @param todoId
    *   The ID of the todo being edited
    * @param currentText
    *   The current text in the edit field
    * @return
    *   IO action for the keydown event
    */
  private def onEditKeyDown(todoId: Int, currentText: String): IO[Unit] = {
    // This will handle Enter (save) and Escape (cancel) keys
    // For now, this is a placeholder
    IO.unit
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
