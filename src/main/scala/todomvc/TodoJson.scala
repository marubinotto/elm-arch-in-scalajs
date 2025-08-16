package todomvc

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.util.{Try, Success, Failure}

/** JSON serialization and deserialization for Todo objects
  */
object TodoJson {

  /** Serialize a list of todos to JSON string
    *
    * @param todos
    *   The list of todos to serialize
    * @return
    *   JSON string representation
    */
  def serializeTodos(todos: List[Todo]): String = {
    val jsArray = js.Array[js.Object]()

    todos.foreach { todo =>
      val jsObj = js.Dynamic
        .literal(
          "id" -> todo.id,
          "text" -> todo.text,
          "completed" -> todo.completed,
          "editing" -> todo.editing
        )
        .asInstanceOf[js.Object]
      jsArray.push(jsObj)
    }

    JSON.stringify(jsArray)
  }

  /** Parse todos from JSON string
    *
    * @param jsonString
    *   The JSON string to parse
    * @return
    *   Try containing the list of todos or failure
    */
  def parseTodos(jsonString: String): Try[List[Todo]] = {
    Try {
      val parsed = JSON.parse(jsonString)

      if (js.Array.isArray(parsed)) {
        val jsArray = parsed.asInstanceOf[js.Array[js.Dynamic]]
        jsArray.toList.map { jsObj =>
          Todo(
            id = jsObj.id.asInstanceOf[Int],
            text = jsObj.text.asInstanceOf[String],
            completed =
              if (js.isUndefined(jsObj.completed)) false
              else jsObj.completed.asInstanceOf[Boolean],
            editing =
              if (js.isUndefined(jsObj.editing)) false
              else jsObj.editing.asInstanceOf[Boolean]
          )
        }
      } else {
        throw new IllegalArgumentException("JSON is not an array")
      }
    }
  }

  /** Serialize a single todo to JSON string
    *
    * @param todo
    *   The todo to serialize
    * @return
    *   JSON string representation
    */
  def serializeTodo(todo: Todo): String = {
    val jsObj = js.Dynamic.literal(
      "id" -> todo.id,
      "text" -> todo.text,
      "completed" -> todo.completed,
      "editing" -> todo.editing
    )
    JSON.stringify(jsObj)
  }

  /** Parse a single todo from JSON string
    *
    * @param jsonString
    *   The JSON string to parse
    * @return
    *   Try containing the todo or failure
    */
  def parseTodo(jsonString: String): Try[Todo] = {
    Try {
      val jsObj = JSON.parse(jsonString).asInstanceOf[js.Dynamic]
      Todo(
        id = jsObj.id.asInstanceOf[Int],
        text = jsObj.text.asInstanceOf[String],
        completed =
          if (js.isUndefined(jsObj.completed)) false
          else jsObj.completed.asInstanceOf[Boolean],
        editing =
          if (js.isUndefined(jsObj.editing)) false
          else jsObj.editing.asInstanceOf[Boolean]
      )
    }
  }

  /** Serialize TodoModel to JSON string (excluding runtime state)
    *
    * @param model
    *   The TodoModel to serialize
    * @return
    *   JSON string representation
    */
  def serializeModel(model: TodoModel): String = {
    val jsObj = js.Dynamic.literal(
      "todos" -> JSON.parse(serializeTodos(model.todoList)),
      "newTodoText" -> model.newTodoText,
      "filter" -> model.filter.displayName,
      "editingTodo" -> model.editingTodo.map(_.id.asInstanceOf[js.Any]).orNull,
      "nextId" -> model.nextId.value
    )
    JSON.stringify(jsObj)
  }

  /** Parse TodoModel from JSON string
    *
    * @param jsonString
    *   The JSON string to parse
    * @return
    *   Try containing the TodoModel or failure
    */
  def parseModel(jsonString: String): Try[TodoModel] = {
    Try {
      val jsObj = JSON.parse(jsonString).asInstanceOf[js.Dynamic]

      val todos = if (jsObj.todos != null) {
        parseTodos(JSON.stringify(jsObj.todos)).getOrElse(List.empty)
      } else {
        List.empty[Todo]
      }

      val filter =
        TodoFilter.fromString(jsObj.filter.asInstanceOf[String]).getOrElse(All)

      val editingTodoId =
        if (jsObj.editingTodo != null && !js.isUndefined(jsObj.editingTodo)) {
          Some(jsObj.editingTodo.asInstanceOf[Int])
        } else {
          None
        }

      val nextId = if (jsObj.nextId != null && !js.isUndefined(jsObj.nextId)) {
        TodoId.unsafe(jsObj.nextId.asInstanceOf[Int])
      } else {
        TodoId(if (todos.isEmpty) 1 else todos.map(_.id).max + 1)
      }

      // Create model with todos and then set editing todo if it exists
      val baseModel = TodoModel
        .withTodos(todos)
        .copy(
          newTodoText = jsObj.newTodoText.asInstanceOf[String],
          filter = filter,
          nextId = nextId
        )

      // Set editing todo if it exists and is valid
      editingTodoId match {
        case Some(id) =>
          baseModel.getTodo(TodoId.unsafe(id)) match {
            case Some(todo) => baseModel.copy(editingTodo = Some(todo))
            case None       => baseModel
          }
        case None => baseModel
      }
    }
  }
}
