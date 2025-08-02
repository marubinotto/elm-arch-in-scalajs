package todomvc

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class TodoModelSpec extends AnyFreeSpec with Matchers {

  val activeTodo1 = Todo.create(1, "Active todo 1")
  val activeTodo2 = Todo.create(2, "Active todo 2")
  val completedTodo = Todo.create(3, "Completed todo").copy(completed = true)
  val editingTodo = Todo.create(4, "Editing todo").copy(editing = true)

  "TodoModel" - {

    "should create initial empty model" in {
      val model = TodoModel.init

      model.todos shouldBe empty
      model.newTodoText shouldBe ""
      model.filter shouldBe All
      model.editingTodo shouldBe None
      model.editText shouldBe ""
    }

    "should create model with initial todos" in {
      val todos = List(activeTodo1, completedTodo)
      val model = TodoModel.withTodos(todos)

      model.todos shouldBe todos
      model.newTodoText shouldBe ""
      model.filter shouldBe All
    }

    "should filter todos correctly" in {
      val todos = List(activeTodo1, activeTodo2, completedTodo)
      val model = TodoModel.init.copy(todos = todos)

      // All filter
      model
        .copy(filter = All)
        .filteredTodos should contain theSameElementsAs todos

      // Active filter
      model
        .copy(filter = Active)
        .filteredTodos should contain theSameElementsAs List(
        activeTodo1,
        activeTodo2
      )

      // Completed filter
      model
        .copy(filter = Completed)
        .filteredTodos should contain theSameElementsAs List(completedTodo)
    }

    "should count active todos correctly" in {
      val todos = List(activeTodo1, activeTodo2, completedTodo)
      val model = TodoModel.init.copy(todos = todos)

      model.activeCount shouldBe 2
    }

    "should count completed todos correctly" in {
      val todos = List(activeTodo1, activeTodo2, completedTodo)
      val model = TodoModel.init.copy(todos = todos)

      model.completedCount shouldBe 1
    }

    "should detect when all todos are completed" in {
      val allCompleted = List(
        completedTodo,
        Todo.create(5, "Another completed").copy(completed = true)
      )
      val mixed = List(activeTodo1, completedTodo)

      TodoModel.init.copy(todos = allCompleted).allCompleted shouldBe true
      TodoModel.init.copy(todos = mixed).allCompleted shouldBe false
      TodoModel.init.allCompleted shouldBe false // empty list
    }

    "should detect when there are completed todos" in {
      val todos = List(activeTodo1, completedTodo)
      val activeTodos = List(activeTodo1, activeTodo2)

      TodoModel.init.copy(todos = todos).hasCompleted shouldBe true
      TodoModel.init.copy(todos = activeTodos).hasCompleted shouldBe false
      TodoModel.init.hasCompleted shouldBe false // empty list
    }

    "should generate next ID correctly" in {
      val todos = List(
        Todo.create(1, "First"),
        Todo.create(3, "Third"),
        Todo.create(2, "Second")
      )
      val model = TodoModel.init.copy(todos = todos)

      model.nextId shouldBe 4
      TodoModel.init.nextId shouldBe 1 // empty list
    }

    "should find todos by ID" in {
      val todos = List(activeTodo1, activeTodo2, completedTodo)
      val model = TodoModel.init.copy(todos = todos)

      model.getTodo(1) shouldBe Some(activeTodo1)
      model.getTodo(3) shouldBe Some(completedTodo)
      model.getTodo(999) shouldBe None
    }

    "should check editing status correctly" in {
      val model = TodoModel.init.copy(editingTodo = Some(1))

      model.isEditing(1) shouldBe true
      model.isEditing(2) shouldBe false

      val notEditingModel = TodoModel.init.copy(editingTodo = None)
      notEditingModel.isEditing(1) shouldBe false
    }
  }
}
