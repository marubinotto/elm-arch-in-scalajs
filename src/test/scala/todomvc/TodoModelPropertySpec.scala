package todomvc

import org.scalatest.propspec.AnyPropSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.{Arbitrary, Gen}
import com.softwaremill.quicklens._

/** Property-based tests for TodoModel invariants and behavior
  */
class TodoModelPropertySpec
    extends AnyPropSpec
    with Matchers
    with ScalaCheckPropertyChecks {

  // Generators for test data
  implicit val arbTodo: Arbitrary[Todo] = Arbitrary(genTodo)
  implicit val arbTodoFilter: Arbitrary[TodoFilter] = Arbitrary(genTodoFilter)
  implicit val arbTodoModel: Arbitrary[TodoModel] = Arbitrary(genTodoModel)

  def genTodo: Gen[Todo] = {
    for {
      id <- Gen.posNum[Int]
      text <- Gen.alphaNumStr.suchThat(_.trim.nonEmpty)
      completed <- Gen.oneOf(true, false)
      editing <- Gen.oneOf(true, false)
    } yield Todo(id, text.trim, completed, editing)
  }

  def genTodoFilter: Gen[TodoFilter] = {
    Gen.oneOf(All, Active, Completed)
  }

  def genTodoModel: Gen[TodoModel] = {
    for {
      todos <- Gen.listOf(genTodo).map(_.distinctBy(_.id)) // Ensure unique IDs
      newTodoText <- Gen.alphaNumStr
      filter <- genTodoFilter
      editingTodo <-
        if (todos.nonEmpty) Gen.option(Gen.oneOf(todos.map(_.id)))
        else Gen.const(None)
      editText <- Gen.alphaNumStr
    } yield TodoModel(todos, newTodoText, filter, editingTodo, editText)
  }

  def genTodoList: Gen[List[Todo]] = {
    Gen.listOf(genTodo).map(_.distinctBy(_.id)) // Ensure unique IDs
  }

  property("todo IDs should always be unique in a model") {
    forAll { (model: TodoModel) =>
      val ids = model.todos.map(_.id)
      ids.distinct.length shouldEqual ids.length
    }
  }

  property("nextId should always be greater than any existing ID") {
    forAll { (model: TodoModel) =>
      whenever(model.todos.nonEmpty) {
        val maxId = model.todos.map(_.id).max
        model.nextId should be > maxId
      }
    }
  }

  property("nextId should be 1 for empty todo list") {
    forAll(Gen.alphaNumStr, genTodoFilter, Gen.alphaNumStr) {
      (newTodoText, filter, editText) =>
        val emptyModel =
          TodoModel(List.empty, newTodoText, filter, None, editText)
        emptyModel.nextId shouldEqual 1
    }
  }

  property("filteredTodos should be a subset of todos") {
    forAll { (model: TodoModel) =>
      val filtered = model.filteredTodos
      filtered.foreach { todo =>
        model.todos should contain(todo)
      }
    }
  }

  property("activeCount should equal the number of non-completed todos") {
    forAll { (model: TodoModel) =>
      val expectedCount = model.todos.count(!_.completed)
      model.activeCount shouldEqual expectedCount
    }
  }

  property("completedCount should equal the number of completed todos") {
    forAll { (model: TodoModel) =>
      val expectedCount = model.todos.count(_.completed)
      model.completedCount shouldEqual expectedCount
    }
  }

  property("activeCount + completedCount should equal total todos") {
    forAll { (model: TodoModel) =>
      model.activeCount + model.completedCount shouldEqual model.todos.length
    }
  }

  property(
    "allCompleted should be true only when all todos are completed and list is non-empty"
  ) {
    forAll { (model: TodoModel) =>
      if (model.todos.isEmpty) {
        model.allCompleted shouldBe false
      } else {
        model.allCompleted shouldEqual model.todos.forall(_.completed)
      }
    }
  }

  property(
    "hasCompleted should be true only when at least one todo is completed"
  ) {
    forAll { (model: TodoModel) =>
      model.hasCompleted shouldEqual model.todos.exists(_.completed)
    }
  }

  property("All filter should include all todos") {
    forAll { (todos: List[Todo]) =>
      val model = TodoModel.init.copy(todos = todos, filter = All)
      model.filteredTodos should contain theSameElementsAs todos
    }
  }

  property("Active filter should include only non-completed todos") {
    forAll { (todos: List[Todo]) =>
      val model = TodoModel.init.copy(todos = todos, filter = Active)
      val expected = todos.filter(!_.completed)
      model.filteredTodos should contain theSameElementsAs expected
    }
  }

  property("Completed filter should include only completed todos") {
    forAll { (todos: List[Todo]) =>
      val model = TodoModel.init.copy(todos = todos, filter = Completed)
      val expected = todos.filter(_.completed)
      model.filteredTodos should contain theSameElementsAs expected
    }
  }

  property("getTodo should return the correct todo for existing IDs") {
    forAll { (model: TodoModel) =>
      model.todos.foreach { todo =>
        model.getTodo(todo.id) shouldEqual Some(todo)
      }
    }
  }

  property("getTodo should return None for non-existing IDs") {
    forAll { (model: TodoModel) =>
      val nonExistingId =
        if (model.todos.isEmpty) 1 else model.todos.map(_.id).max + 100
      model.getTodo(nonExistingId) shouldEqual None
    }
  }

  property("isEditing should return true only for the editing todo") {
    forAll { (model: TodoModel) =>
      model.editingTodo match {
        case Some(editingId) =>
          model.isEditing(editingId) shouldBe true
          model.todos.filter(_.id != editingId).foreach { todo =>
            model.isEditing(todo.id) shouldBe false
          }
        case None =>
          model.todos.foreach { todo =>
            model.isEditing(todo.id) shouldBe false
          }
      }
    }
  }

  property("model should remain consistent after adding todos") {
    forAll { (model: TodoModel, newTodoText: String) =>
      whenever(newTodoText.trim.nonEmpty) {
        val newId = model.nextId
        val newTodo = Todo.create(newId, newTodoText.trim)
        val updatedModel = model.modify(_.todos).using(_ :+ newTodo)

        // Check invariants
        val ids = updatedModel.todos.map(_.id)
        ids.distinct.length shouldEqual ids.length
        updatedModel.todos should contain(newTodo)
        updatedModel.todos.length shouldEqual model.todos.length + 1
      }
    }
  }

  property("model should remain consistent after removing todos") {
    forAll { (model: TodoModel) =>
      whenever(model.todos.nonEmpty) {
        val todoToRemove = model.todos.head
        val updatedModel =
          model.modify(_.todos).using(_.filterNot(_.id == todoToRemove.id))

        // Check invariants
        val ids = updatedModel.todos.map(_.id)
        ids.distinct.length shouldEqual ids.length
        updatedModel.todos should not contain todoToRemove
        updatedModel.todos.length shouldEqual model.todos.length - 1
      }
    }
  }

  property("model should remain consistent after toggling todo completion") {
    forAll { (model: TodoModel) =>
      whenever(model.todos.nonEmpty) {
        val todoToToggle = model.todos.head
        val updatedModel = model
          .modify(_.todos.eachWhere(_.id == todoToToggle.id).completed)
          .using(!_)

        // Check invariants
        val ids = updatedModel.todos.map(_.id)
        ids.distinct.length shouldEqual ids.length
        updatedModel.todos.length shouldEqual model.todos.length

        val updatedTodo = updatedModel.getTodo(todoToToggle.id).get
        updatedTodo.completed shouldEqual !todoToToggle.completed
        updatedTodo.id shouldEqual todoToToggle.id
        updatedTodo.text shouldEqual todoToToggle.text
      }
    }
  }

  property("filter changes should not affect todo data") {
    forAll { (model: TodoModel, newFilter: TodoFilter) =>
      val updatedModel = model.copy(filter = newFilter)

      updatedModel.todos should contain theSameElementsAs model.todos
      updatedModel.newTodoText shouldEqual model.newTodoText
      updatedModel.editingTodo shouldEqual model.editingTodo
      updatedModel.editText shouldEqual model.editText
    }
  }

  property("editing state changes should not affect other todos") {
    forAll { (model: TodoModel) =>
      whenever(model.todos.nonEmpty) {
        val todoToEdit = model.todos.head
        val updatedModel = model.copy(
          editingTodo = Some(todoToEdit.id),
          editText = "editing text"
        )

        // Other todos should remain unchanged
        model.todos.filter(_.id != todoToEdit.id).foreach { todo =>
          updatedModel.getTodo(todo.id) shouldEqual Some(todo)
        }

        // Editing todo should remain unchanged in the todos list
        updatedModel.getTodo(todoToEdit.id) shouldEqual Some(todoToEdit)
      }
    }
  }

  property(
    "model invariants should hold after any sequence of valid operations"
  ) {
    forAll { (initialModel: TodoModel, operations: List[String]) =>
      val finalModel = operations.foldLeft(initialModel) { (model, op) =>
        op match {
          case "add" if model.newTodoText.trim.nonEmpty =>
            val newId = model.nextId
            val newTodo = Todo.create(newId, model.newTodoText.trim)
            model
              .modify(_.todos)
              .using(_ :+ newTodo)
              .modify(_.newTodoText)
              .setTo("")
          case "toggle" if model.todos.nonEmpty =>
            val todoId = model.todos.head.id
            model.modify(_.todos.eachWhere(_.id == todoId).completed).using(!_)
          case "delete" if model.todos.nonEmpty =>
            val todoId = model.todos.head.id
            model.modify(_.todos).using(_.filterNot(_.id == todoId))
          case "filter_all" =>
            model.copy(filter = All)
          case "filter_active" =>
            model.copy(filter = Active)
          case "filter_completed" =>
            model.copy(filter = Completed)
          case _ =>
            model // No-op for invalid operations
        }
      }

      // Check all invariants still hold
      val ids = finalModel.todos.map(_.id)
      ids.distinct.length shouldEqual ids.length
      finalModel.activeCount + finalModel.completedCount shouldEqual finalModel.todos.length
      finalModel.filteredTodos.foreach(finalModel.todos should contain(_))
    }
  }
}
