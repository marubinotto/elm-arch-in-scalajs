package todomvc

import architecture.ValidationError

/** Validation utilities for TodoMVC */
object TodoValidation {

  /** Validate todo text */
  def validateTodoText(text: String): Either[ValidationError, String] = {
    val trimmed = text.trim
    if (trimmed.isEmpty) {
      Left(ValidationError("text", "Todo text cannot be empty", text))
    } else if (trimmed.length > 1000) {
      Left(
        ValidationError(
          "text",
          "Todo text is too long (max 1000 characters)",
          text
        )
      )
    } else if (trimmed.contains('\n') || trimmed.contains('\r')) {
      Left(
        ValidationError("text", "Todo text cannot contain line breaks", text)
      )
    } else {
      Right(trimmed)
    }
  }

  /** Validate todo ID */
  def validateTodoId(id: Int): Either[ValidationError, Int] = {
    if (id <= 0) {
      Left(ValidationError("id", "Todo ID must be positive", id))
    } else {
      Right(id)
    }
  }

  /** Validate that a todo exists in the model */
  def validateTodoExists(
      id: Int,
      model: TodoModel
  ): Either[ValidationError, Todo] = {
    model.getTodo(id) match {
      case Some(todo) => Right(todo)
      case None =>
        Left(ValidationError("id", s"Todo with ID $id does not exist", id))
    }
  }

  /** Validate todo list for consistency */
  def validateTodoList(
      todos: List[Todo]
  ): Either[ValidationError, List[Todo]] = {
    // Check for duplicate IDs
    val ids = todos.map(_.id)
    val duplicateIds = ids.groupBy(identity).filter(_._2.length > 1).keys.toList

    if (duplicateIds.nonEmpty) {
      Left(
        ValidationError(
          "todos",
          s"Duplicate todo IDs found: ${duplicateIds.mkString(", ")}",
          todos
        )
      )
    } else if (todos.exists(_.id <= 0)) {
      Left(ValidationError("todos", "All todo IDs must be positive", todos))
    } else if (todos.length > 10000) {
      Left(ValidationError("todos", "Too many todos (max 10000)", todos))
    } else {
      Right(todos)
    }
  }

  /** Validate filter */
  def validateFilter(
      filter: TodoFilter
  ): Either[ValidationError, TodoFilter] = {
    // All TodoFilter instances are valid by construction
    Right(filter)
  }

  /** Validate entire model for consistency */
  def validateModel(model: TodoModel): Either[ValidationError, TodoModel] = {
    for {
      validTodos <- validateTodoList(model.todos)
      validFilter <- validateFilter(model.filter)
      _ <- model.editingTodo match {
        case Some(id) => validateTodoExists(id, model)
        case None     => Right(())
      }
    } yield model.copy(todos = validTodos, filter = validFilter)
  }

  /** Safe model update that validates the result */
  def safeUpdateModel(
      model: TodoModel,
      update: TodoModel => TodoModel,
      fallback: TodoModel
  ): TodoModel = {
    try {
      val updatedModel = update(model)
      validateModel(updatedModel) match {
        case Right(validModel) => validModel
        case Left(error) =>
          org.scalajs.dom.console
            .warn(s"Model validation failed: ${error.userMessage}")
          fallback
      }
    } catch {
      case error: Throwable =>
        org.scalajs.dom.console
          .error(s"Model update failed: ${error.getMessage}")
        fallback
    }
  }

  /** Validate and sanitize input text */
  def sanitizeInput(input: String): String = {
    if (input == null) {
      ""
    } else {
      // Remove control characters and normalize whitespace
      input
        .replaceAll(
          "[\u0000-\u001F\u007F-\u009F]",
          ""
        ) // Remove control characters
        .replaceAll("\\s+", " ") // Normalize whitespace
        .trim
    }
  }

  /** Check if model state is consistent */
  def isModelConsistent(model: TodoModel): Boolean = {
    validateModel(model).isRight
  }

  /** Get validation errors for a model */
  def getModelErrors(model: TodoModel): List[ValidationError] = {
    validateModel(model) match {
      case Left(error) => List(error)
      case Right(_)    => List.empty
    }
  }
}
