package todomvc

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class TodoSpec extends AnyFreeSpec with Matchers {

  "Todo" - {

    "should create a new todo with correct defaults" in {
      val todo = Todo.create(1, "Test todo")

      todo.id shouldBe 1
      todo.text shouldBe "Test todo"
      todo.completed shouldBe false
      todo.editing shouldBe false
    }

    "should allow creating todos with different IDs and text" in {
      val todo1 = Todo.create(1, "First todo")
      val todo2 = Todo.create(2, "Second todo")

      todo1.id shouldBe 1
      todo1.text shouldBe "First todo"
      todo2.id shouldBe 2
      todo2.text shouldBe "Second todo"
    }

    "should be immutable" in {
      val original = Todo.create(1, "Original")
      val modified = original.copy(completed = true)

      original.completed shouldBe false
      modified.completed shouldBe true
    }
  }
}
