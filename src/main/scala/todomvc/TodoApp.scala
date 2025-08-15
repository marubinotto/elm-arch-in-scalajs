package todomvc

import architecture._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.softwaremill.quicklens._
import vdom.VNode
import todomvc.{All, Active, Completed}
import scala.concurrent.duration._

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

      // Update todo text only if still editing (for blur events)
      case UpdateTodoIfEditing(id, text) =>
        if (model.isEditing(id)) {
          // If still editing, treat it like a regular UpdateTodo
          update(UpdateTodo(id, text), model)
        } else {
          // If not editing anymore (e.g., after Escape), ignore the blur event
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
    * @param dispatch
    *   Optional function to dispatch messages for interactive elements
    * @return
    *   Virtual DOM representation of the UI
    */
  override def view(
      model: TodoModel,
      dispatch: Option[TodoMsg => IO[Unit]] = None
  ): VNode = {
    import vdom.Html._

    // Since we can't create fragments, we'll create a div that acts as a container
    // but we'll make sure the CSS and structure work correctly
    div("style" -> "display: contents;")(
      dispatch match {
        case Some(dispatchFn) =>
          List(
            renderHeaderWithDispatch(model, dispatchFn),
            renderMainWithDispatch(model, dispatchFn),
            renderFooterWithDispatch(model, dispatchFn)
          )
        case None =>
          List(
            renderHeader(model),
            renderMain(model),
            renderFooter(model)
          )
      }*
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
        "class" -> "new-todo",
        "placeholder" -> "What needs to be done?",
        "value" -> model.newTodoText,
        "autofocus" -> "true"
      )
    )
  }

  /** Render the header section with new todo input and message dispatch
    *
    * @param model
    *   The current model state
    * @param dispatch
    *   Function to dispatch messages
    * @return
    *   Virtual DOM node for the header
    */
  private def renderHeaderWithDispatch(
      model: TodoModel,
      dispatch: TodoMsg => IO[Unit]
  ): VNode = {
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
          onInputValue(UpdateNewTodoText.apply, dispatch),
          "keydown" -> { (event: org.scalajs.dom.Event) =>
            for {
              keyEvent <- IO.delay(
                event.asInstanceOf[org.scalajs.dom.KeyboardEvent]
              )
              _ <-
                if (keyEvent.keyCode == 13) { // Enter key
                  for {
                    _ <- IO.delay(keyEvent.preventDefault())
                    target <- IO.delay(
                      event.target
                        .asInstanceOf[org.scalajs.dom.HTMLInputElement]
                    )
                    text <- IO.delay(target.value.trim)
                    _ <-
                      if (text.nonEmpty) {
                        for {
                          // Clear the input field immediately to prevent duplicate submissions
                          _ <- IO.delay(target.value = "")
                          // Small delay to ensure DOM is updated
                          _ <- IO.sleep(1.millis)
                          // Then dispatch the AddTodo message
                          _ <- dispatch(AddTodo(text))
                        } yield ()
                      } else IO.unit
                  } yield ()
                } else IO.unit
            } yield ()
          }
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
          (Map(
            "id" -> "toggle-all",
            "class" -> "toggle-all",
            "type" -> "checkbox"
          ) ++ (if (model.allCompleted) Map("checked" -> "checked")
                else Map.empty)).toSeq*
        ),
        label("for" -> "toggle-all")(text("Mark all as complete")),
        ul("class" -> "todo-list")(
          model.filteredTodos.map(renderTodoItem(_, model))*
        )
      )
    }
  }

  /** Render the main section with todo list and message dispatch
    *
    * @param model
    *   The current model state
    * @param dispatch
    *   Function to dispatch messages
    * @return
    *   Virtual DOM node for the main section
    */
  private def renderMainWithDispatch(
      model: TodoModel,
      dispatch: TodoMsg => IO[Unit]
  ): VNode = {
    import vdom.Html._
    import vdom.Events._

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
            onChangeMsg(ToggleAll, dispatch)
          )
        ),
        label("for" -> "toggle-all")(text("Mark all as complete")),
        ul("class" -> "todo-list")(
          model.filteredTodos.map(
            renderTodoItemWithDispatch(_, model, dispatch)
          )*
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
          (Map(
            "class" -> "toggle",
            "type" -> "checkbox"
          ) ++ (if (todo.completed) Map("checked" -> "checked")
                else Map.empty)).toSeq*
        ),
        label()(text(todo.text)),
        button("class" -> "destroy")()
      ),
      if (model.isEditing(todo.id)) {
        input(
          "class" -> "edit",
          "value" -> model.editText
        )
      } else {
        text("") // Empty node when not editing
      }
    )
  }

  /** Render an individual todo item with message dispatch
    *
    * @param todo
    *   The todo item to render
    * @param model
    *   The current model state
    * @param dispatch
    *   Function to dispatch messages
    * @return
    *   Virtual DOM node for the todo item
    */
  private def renderTodoItemWithDispatch(
      todo: Todo,
      model: TodoModel,
      dispatch: TodoMsg => IO[Unit]
  ): VNode = {
    import vdom.Html._
    import vdom.Events._

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
            onChangeMsg(ToggleTodo(todo.id), dispatch),
            "click" -> { (event: org.scalajs.dom.Event) =>
              IO.delay(
                org.scalajs.dom.console
                  .log(s"Click on checkbox for todo ${todo.id}")
              )
            }
          )
        ),
        label(
          Map.empty,
          Map(
            onDoubleClickMsg(EditTodo(todo.id), dispatch)
          )
        )(text(todo.text)),
        button(
          Map("class" -> "destroy"),
          Map(
            onClickMsg(DeleteTodo(todo.id), dispatch)
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
            onInputValue(UpdateEditText.apply, dispatch),
            "keydown" -> { (event: org.scalajs.dom.Event) =>
              for {
                keyEvent <- IO.delay(
                  event.asInstanceOf[org.scalajs.dom.KeyboardEvent]
                )
                _ <- keyEvent.keyCode match {
                  case 13 => // Enter
                    for {
                      _ <- IO.delay(keyEvent.preventDefault())
                      target <- IO.delay(
                        event.target
                          .asInstanceOf[org.scalajs.dom.HTMLInputElement]
                      )
                      _ <- dispatch(UpdateTodo(todo.id, target.value))
                    } yield ()
                  case 27 => // Escape
                    for {
                      _ <- IO.delay(keyEvent.preventDefault())
                      _ <- dispatch(CancelEdit)
                    } yield ()
                  case _ => // Other keys - do nothing
                    IO.unit
                }
              } yield ()
            },
            onBlurValue(
              (text: String) => UpdateTodoIfEditing(todo.id, text),
              dispatch
            )
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
          button("class" -> "clear-completed")(text("Clear completed"))
        } else {
          text("") // Empty node when no completed todos
        }
      )
    }
  }

  /** Render the footer section with filters and stats and message dispatch
    *
    * @param model
    *   The current model state
    * @param dispatch
    *   Function to dispatch messages
    * @return
    *   Virtual DOM node for the footer
    */
  private def renderFooterWithDispatch(
      model: TodoModel,
      dispatch: TodoMsg => IO[Unit]
  ): VNode = {
    import vdom.Html._
    import vdom.Events._

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
          TodoFilter.all.map(
            renderFilterButtonWithDispatch(_, model.filter, dispatch)
          )*
        ),
        if (model.hasCompleted) {
          button(
            Map("class" -> "clear-completed"),
            Map(
              onClickMsg(ClearCompleted, dispatch)
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
        (Map(
          "href" -> s"#/${filter.displayName.toLowerCase}"
        ) ++ (if (filter == currentFilter) Map("class" -> "selected")
              else Map.empty)).toSeq*
      )(text(filter.displayName))
    )
  }

  /** Render a filter button with message dispatch
    *
    * @param filter
    *   The filter option
    * @param currentFilter
    *   The currently selected filter
    * @param dispatch
    *   Function to dispatch messages
    * @return
    *   Virtual DOM node for the filter button
    */
  private def renderFilterButtonWithDispatch(
      filter: TodoFilter,
      currentFilter: TodoFilter,
      dispatch: TodoMsg => IO[Unit]
  ): VNode = {
    import vdom.Html._
    import vdom.Events._

    li()(
      a(
        Map(
          "href" -> s"#/${filter.displayName.toLowerCase}"
        ) ++ (if (filter == currentFilter) Map("class" -> "selected")
              else Map.empty),
        Map(
          "click" -> { (event: org.scalajs.dom.Event) =>
            for {
              _ <- IO.delay(event.preventDefault())
              _ <- dispatch(SetFilter(filter))
            } yield ()
          }
        )
      )(text(filter.displayName))
    )
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
