package todomvc

import architecture._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.softwaremill.quicklens._
import vdom.VNode
import todomvc.{All, Active, Completed}

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

      // Add a new todo with comprehensive validation
      case AddTodo(text) =>
        TodoValidation.validateTodoText(text) match {
          case Right(validText) =>
            try {
              val newId = model.nextId
              TodoValidation.validateTodoId(newId) match {
                case Right(validId) =>
                  val newTodo = Todo.create(validId, validText)
                  val updatedModel = TodoValidation.safeUpdateModel(
                    model,
                    _.modify(_.todos)
                      .using(_ :+ newTodo)
                      .modify(_.newTodoText)
                      .setTo(""),
                    model
                  )
                  Update(
                    updatedModel,
                    Cmd.task(saveTodosToStorage(updatedModel.todos))
                  )
                case Left(error) =>
                  org.scalajs.dom.console
                    .error(s"Invalid todo ID: ${error.userMessage}")
                  Update(model, Cmd.none)
              }
            } catch {
              case error: Throwable =>
                org.scalajs.dom.console
                  .error(s"Failed to add todo: ${error.getMessage}")
                Update(model, Cmd.none)
            }
          case Left(error) =>
            org.scalajs.dom.console
              .warn(s"Invalid todo text: ${error.userMessage}")
            Update(model, Cmd.none)
        }

      // Toggle the completed status of a todo with validation
      case ToggleTodo(id) =>
        TodoValidation.validateTodoExists(id, model) match {
          case Right(_) =>
            val updatedModel = TodoValidation.safeUpdateModel(
              model,
              _.modify(_.todos.eachWhere(_.id == id).completed).using(!_),
              model
            )
            Update(
              updatedModel,
              Cmd.task(saveTodosToStorage(updatedModel.todos))
            )
          case Left(error) =>
            org.scalajs.dom.console
              .warn(s"Cannot toggle todo: ${error.userMessage}")
            Update(model, Cmd.none)
        }

      // Update the new todo input text with sanitization
      case UpdateNewTodoText(text) =>
        val sanitizedText = TodoValidation.sanitizeInput(text)
        val updatedModel = TodoValidation.safeUpdateModel(
          model,
          _.modify(_.newTodoText).setTo(sanitizedText),
          model
        )
        Update(updatedModel, Cmd.none)

      // Set the current filter for todo visibility
      case SetFilter(filter) =>
        val updatedModel = model.modify(_.filter).setTo(filter)
        Update(updatedModel, Cmd.none)

      // Load todos from storage with validation
      case LoadTodos(todos) =>
        TodoValidation.validateTodoList(todos) match {
          case Right(validTodos) =>
            val updatedModel = TodoValidation.safeUpdateModel(
              model,
              _.modify(_.todos).setTo(validTodos),
              model
            )
            Update(updatedModel, Cmd.none)
          case Left(error) =>
            org.scalajs.dom.console
              .error(s"Invalid todos loaded from storage: ${error.userMessage}")
            // Use empty list as fallback
            val updatedModel = TodoValidation.safeUpdateModel(
              model,
              _.modify(_.todos).setTo(List.empty),
              model
            )
            Update(updatedModel, Cmd.none)
        }

      // Handle save completion (no model change needed)
      case SaveComplete =>
        Update(model, Cmd.none)

      // Delete a todo by ID with validation
      case DeleteTodo(id) =>
        TodoValidation.validateTodoExists(id, model) match {
          case Right(_) =>
            val updatedModel = TodoValidation.safeUpdateModel(
              model,
              _.modify(_.todos).using(_.filterNot(_.id == id)),
              model
            )
            Update(
              updatedModel,
              Cmd.task(saveTodosToStorage(updatedModel.todos))
            )
          case Left(error) =>
            org.scalajs.dom.console
              .warn(s"Cannot delete todo: ${error.userMessage}")
            Update(model, Cmd.none)
        }

      // Enter edit mode for a specific todo with validation
      case EditTodo(id) =>
        TodoValidation.validateTodoExists(id, model) match {
          case Right(todo) =>
            val updatedModel = TodoValidation.safeUpdateModel(
              model,
              _.modify(_.editingTodo)
                .setTo(Some(id))
                .modify(_.editText)
                .setTo(todo.text)
                .modify(_.todos.eachWhere(_.id == id).editing)
                .setTo(true),
              model
            )
            Update(updatedModel, Cmd.none)
          case Left(error) =>
            org.scalajs.dom.console
              .warn(s"Cannot edit todo: ${error.userMessage}")
            Update(model, Cmd.none)
        }

      // Update a todo's text with comprehensive validation
      case UpdateTodo(id, text) =>
        TodoValidation.validateTodoExists(id, model) match {
          case Right(_) =>
            TodoValidation.validateTodoText(text) match {
              case Right(validText) =>
                val updatedModel = TodoValidation.safeUpdateModel(
                  model,
                  _.modify(_.todos.eachWhere(_.id == id))
                    .using(_.copy(text = validText, editing = false))
                    .modify(_.editingTodo)
                    .setTo(None)
                    .modify(_.editText)
                    .setTo(""),
                  model
                )
                Update(
                  updatedModel,
                  Cmd.task(saveTodosToStorage(updatedModel.todos))
                )
              case Left(_) =>
                // Empty or invalid text - delete the todo instead
                val updatedModel = TodoValidation.safeUpdateModel(
                  model,
                  _.modify(_.todos)
                    .using(_.filterNot(_.id == id))
                    .modify(_.editingTodo)
                    .setTo(None)
                    .modify(_.editText)
                    .setTo(""),
                  model
                )
                Update(
                  updatedModel,
                  Cmd.task(saveTodosToStorage(updatedModel.todos))
                )
            }
          case Left(error) =>
            org.scalajs.dom.console
              .warn(s"Cannot update todo: ${error.userMessage}")
            Update(model, Cmd.none)
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

      // Update the edit input text with sanitization
      case UpdateEditText(text) =>
        val sanitizedText = TodoValidation.sanitizeInput(text)
        val updatedModel = TodoValidation.safeUpdateModel(
          model,
          _.modify(_.editText).setTo(sanitizedText),
          model
        )
        Update(updatedModel, Cmd.none)

      // Toggle all todos between completed and not completed
      case ToggleAll =>
        if (model.todos.nonEmpty) {
          val shouldComplete = !model.allCompleted
          val updatedModel = TodoValidation.safeUpdateModel(
            model,
            _.modify(_.todos.each.completed).setTo(shouldComplete),
            model
          )
          Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))
        } else {
          // No todos to toggle
          Update(model, Cmd.none)
        }

      // Remove all completed todos
      case ClearCompleted =>
        if (model.hasCompleted) {
          val updatedModel = TodoValidation.safeUpdateModel(
            model,
            _.modify(_.todos).using(_.filterNot(_.completed)),
            model
          )
          Update(updatedModel, Cmd.task(saveTodosToStorage(updatedModel.todos)))
        } else {
          // No completed todos to clear
          Update(model, Cmd.none)
        }

      // Trigger auto-save operation
      case AutoSave =>
        Update(model, Cmd.task(saveTodosToStorage(model.todos)))

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

    // Since we can't create fragments, we'll create a div that acts as a container
    // but we'll make sure the CSS and structure work correctly
    div("style" -> "display: contents;")(
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
    import vdom.Events._

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
          "input" -> onInputText(UpdateNewTodoText.apply),
          "keydown" -> onEnterKey(AddTodo(model.newTodoText))
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
            "change" -> onToggleAll
          )
        ),
        label("for" -> "toggle-all")(text("Mark all as complete")),
        ul("class" -> "todo-list")(
          model.filteredTodos.map(renderTodoItem(_, model))*
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

    li("class" -> classes, "data-id" -> todo.id.toString)(
      div("class" -> "view")(
        input(
          Map(
            "class" -> "toggle",
            "type" -> "checkbox"
          ) ++ (if (todo.completed) Map("checked" -> "checked") else Map.empty),
          Map(
            "change" -> onToggleTodo(todo.id)
          )
        ),
        label(
          Map.empty,
          Map(
            "dblclick" -> onEditTodo(todo.id)
          )
        )(text(todo.text)),
        button(
          Map("class" -> "destroy"),
          Map(
            "click" -> onDeleteTodo(todo.id)
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
            "input" -> onInputText(UpdateEditText.apply),
            "keydown" -> onEditKeyDown(todo.id),
            "blur" -> onUpdateTodo(todo.id, model.editText)
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
          TodoFilter.all.map(renderFilterButton(_, model.filter))*
        ),
        if (model.hasCompleted) {
          button(
            Map("class" -> "clear-completed"),
            Map(
              "click" -> onClearCompleted
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
          "click" -> onSetFilter(filter)
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

  // Global dispatch function - will be set by the runtime
  private var dispatchFn: Option[TodoMsg => IO[Unit]] = None

  /** Set the dispatch function (called by the runtime) */
  def setDispatch(dispatch: TodoMsg => IO[Unit]): Unit = {
    dispatchFn = Some(dispatch)
  }

  /** Safely dispatch a message */
  private def safeDispatch(msg: TodoMsg): IO[Unit] = {
    dispatchFn.map(_(msg)).getOrElse(IO.unit)
  }

  /** Create event handlers that dispatch messages */

  // These event handlers are now just placeholders since real event handling
  // is done in attachEventListeners
  private def onInputText(msgFactory: String => TodoMsg): IO[Unit] = IO.unit
  private def onEnterKey(msgFactory: => TodoMsg): IO[Unit] = IO.unit
  private def onEditKeyDown(todoId: Int): IO[Unit] = IO.unit

  private def onToggleAll: IO[Unit] = safeDispatch(ToggleAll)
  private def onToggleTodo(id: Int): IO[Unit] = safeDispatch(ToggleTodo(id))
  private def onEditTodo(id: Int): IO[Unit] = safeDispatch(EditTodo(id))
  private def onDeleteTodo(id: Int): IO[Unit] = safeDispatch(DeleteTodo(id))
  private def onUpdateTodo(id: Int, text: String): IO[Unit] = safeDispatch(
    UpdateTodo(id, text)
  )
  private def onClearCompleted: IO[Unit] = safeDispatch(ClearCompleted)
  private def onSetFilter(filter: TodoFilter): IO[Unit] = safeDispatch(
    SetFilter(filter)
  )

  /** Attach real DOM event listeners after rendering This is called by the
    * Runtime after each render
    */
  def attachEventListeners(
      container: org.scalajs.dom.Element,
      model: TodoModel,
      msgQueue: cats.effect.std.Queue[IO, TodoMsg]
  ): IO[Unit] = {
    import org.scalajs.dom
    import org.scalajs.dom.{HTMLInputElement, KeyboardEvent}

    IO.delay {
      org.scalajs.dom.console.log(
        s"[TodoApp] Attaching event listeners, todos count: ${model.todos.length}"
      )
      org.scalajs.dom.console
        .log(s"[TodoApp] Container: ${container.tagName}, id: ${container.id}")

      // Remove existing event listeners first
      // (In a real implementation, we'd track these properly)

      // New todo input
      val newTodoInput =
        container.querySelector(".new-todo").asInstanceOf[HTMLInputElement]
      org.scalajs.dom.console
        .log(s"[TodoApp] Found new todo input: ${newTodoInput != null}")
      if (newTodoInput != null) {
        // Clear existing listeners by cloning the element
        val newInput =
          newTodoInput.cloneNode(true).asInstanceOf[HTMLInputElement]
        newTodoInput.parentNode.replaceChild(newInput, newTodoInput)

        // Add input event listener
        newInput.addEventListener(
          "input",
          (e: dom.Event) => {
            val target = e.target.asInstanceOf[HTMLInputElement]
            val value = target.value
            msgQueue.offer(UpdateNewTodoText(value)).unsafeRunAsync(_ => ())
          }
        )

        // Add keydown event listener for Enter key
        newInput.addEventListener(
          "keydown",
          (e: dom.Event) => {
            val keyEvent = e.asInstanceOf[KeyboardEvent]
            if (keyEvent.keyCode == 13) { // Enter key
              val target = e.target.asInstanceOf[HTMLInputElement]
              val text = target.value.trim
              if (text.nonEmpty) {
                org.scalajs.dom.console
                  .log(s"[TodoApp] Dispatching AddTodo: $text")
                msgQueue.offer(AddTodo(text)).unsafeRunAsync(_ => ())
                target.value = ""
              }
            }
          }
        )
      }

      // Toggle all checkbox
      val toggleAll = container.querySelector(".toggle-all")
      if (toggleAll != null) {
        val newToggleAll = toggleAll.cloneNode(true).asInstanceOf[dom.Element]
        toggleAll.parentNode.replaceChild(newToggleAll, toggleAll)

        newToggleAll.addEventListener(
          "change",
          (_: dom.Event) => {
            msgQueue.offer(ToggleAll).unsafeRunAsync(_ => ())
          }
        )
      }

      // Individual todo items
      val todoList = container.querySelector(".todo-list")
      if (todoList != null) {
        val newTodoList = todoList.cloneNode(true).asInstanceOf[dom.Element]
        todoList.parentNode.replaceChild(newTodoList, todoList)

        // Add event delegation for todo items
        newTodoList.addEventListener(
          "click",
          (e: dom.Event) => {
            val target = e.target.asInstanceOf[dom.Element]
            val li = target.closest("li")
            if (li != null) {
              val todoId = li.getAttribute("data-id").toInt

              if (target.classList.contains("toggle")) {
                msgQueue.offer(ToggleTodo(todoId)).unsafeRunAsync(_ => ())
              } else if (target.classList.contains("destroy")) {
                msgQueue.offer(DeleteTodo(todoId)).unsafeRunAsync(_ => ())
              }
            }
          }
        )

        // Double-click to edit
        newTodoList.addEventListener(
          "dblclick",
          (e: dom.Event) => {
            val target = e.target.asInstanceOf[dom.Element]
            if (target.tagName.toLowerCase == "label") {
              val li = target.closest("li")
              if (li != null) {
                val todoId = li.getAttribute("data-id").toInt
                msgQueue.offer(EditTodo(todoId)).unsafeRunAsync(_ => ())
              }
            }
          }
        )

        // Edit input handling
        newTodoList.addEventListener(
          "keydown",
          (e: dom.Event) => {
            val target = e.target.asInstanceOf[dom.Element]
            if (target.classList.contains("edit")) {
              val keyEvent = e.asInstanceOf[KeyboardEvent]
              val li = target.closest("li")
              if (li != null) {
                val todoId = li.getAttribute("data-id").toInt
                val input = target.asInstanceOf[HTMLInputElement]

                if (keyEvent.keyCode == 13) { // Enter
                  msgQueue
                    .offer(UpdateTodo(todoId, input.value))
                    .unsafeRunAsync(_ => ())
                } else if (keyEvent.keyCode == 27) { // Escape
                  msgQueue.offer(CancelEdit).unsafeRunAsync(_ => ())
                }
              }
            }
          }
        )

        // Edit input blur
        newTodoList.addEventListener(
          "blur",
          (e: dom.Event) => {
            val target = e.target.asInstanceOf[dom.Element]
            if (target.classList.contains("edit")) {
              val li = target.closest("li")
              if (li != null) {
                val todoId = li.getAttribute("data-id").toInt
                val input = target.asInstanceOf[HTMLInputElement]
                msgQueue
                  .offer(UpdateTodo(todoId, input.value))
                  .unsafeRunAsync(_ => ())
              }
            }
          },
          true
        ) // Use capture
      }

      // Filter links
      val filters = container.querySelector(".filters")
      if (filters != null) {
        val newFilters = filters.cloneNode(true).asInstanceOf[dom.Element]
        filters.parentNode.replaceChild(newFilters, filters)

        newFilters.addEventListener(
          "click",
          (e: dom.Event) => {
            val target = e.target.asInstanceOf[dom.Element]
            if (target.tagName.toLowerCase == "a") {
              e.preventDefault()
              val href = target.getAttribute("href")
              val filterName = href.substring(2) // Remove "#/"
              val filter = filterName match {
                case "active"    => Active
                case "completed" => Completed
                case _           => All
              }
              msgQueue.offer(SetFilter(filter)).unsafeRunAsync(_ => ())
            }
          }
        )
      }

      // Clear completed button
      val clearCompleted = container.querySelector(".clear-completed")
      if (clearCompleted != null) {
        val newClearCompleted =
          clearCompleted.cloneNode(true).asInstanceOf[dom.Element]
        clearCompleted.parentNode.replaceChild(
          newClearCompleted,
          clearCompleted
        )

        newClearCompleted.addEventListener(
          "click",
          (_: dom.Event) => {
            msgQueue.offer(ClearCompleted).unsafeRunAsync(_ => ())
          }
        )
      }
    }
  }

  // Private helper functions for side effects

  /** Load todos from local storage
    *
    * @return
    *   IO operation that loads todos and returns a LoadTodos message
    */
  private def loadTodosFromStorage: IO[TodoMsg] = {
    LocalStorage.loadTodos
      .map(LoadTodos.apply)
      .handleErrorWith { error =>
        // Log error and return empty list for graceful degradation
        IO.delay(
          org.scalajs.dom.console
            .error(s"Failed to load todos: ${error.getMessage}")
        ) *>
          IO.pure(LoadTodos(List.empty))
      }
  }

  /** Save todos to local storage
    *
    * @param todos
    *   The todos to save
    * @return
    *   IO operation that saves todos and returns a SaveComplete message
    */
  private def saveTodosToStorage(todos: List[Todo]): IO[TodoMsg] = {
    LocalStorage
      .saveTodos(todos)
      .as(SaveComplete)
      .handleErrorWith { error =>
        // Log error and return network error message
        IO.delay(
          org.scalajs.dom.console
            .error(s"Failed to save todos: ${error.getMessage}")
        ) *>
          IO.pure(
            SaveComplete
          ) // Just complete successfully even on error for graceful degradation
      }
  }
}
