package todomvc

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class TodoFilterSpec extends AnyFreeSpec with Matchers {

  val activeTodo = Todo.create(1, "Active todo")
  val completedTodo = Todo.create(2, "Completed todo").copy(completed = true)

  "All filter" - {

    "should have correct display name" in {
      All.displayName shouldBe "All"
    }

    "should match all todos" in {
      All.matches(activeTodo) shouldBe true
      All.matches(completedTodo) shouldBe true
    }
  }

  "Active filter" - {

    "should have correct display name" in {
      Active.displayName shouldBe "Active"
    }

    "should match only active todos" in {
      Active.matches(activeTodo) shouldBe true
      Active.matches(completedTodo) shouldBe false
    }
  }

  "Completed filter" - {

    "should have correct display name" in {
      Completed.displayName shouldBe "Completed"
    }

    "should match only completed todos" in {
      Completed.matches(activeTodo) shouldBe false
      Completed.matches(completedTodo) shouldBe true
    }
  }

  "TodoFilter companion object" - {

    "should contain all filter options" in {
      TodoFilter.all should contain theSameElementsAs List(
        All,
        Active,
        Completed
      )
    }

    "should parse filters from strings" in {
      TodoFilter.fromString("all") shouldBe Some(All)
      TodoFilter.fromString("active") shouldBe Some(Active)
      TodoFilter.fromString("completed") shouldBe Some(Completed)
      TodoFilter.fromString("ALL") shouldBe Some(All) // case insensitive
      TodoFilter.fromString("invalid") shouldBe None
    }
  }
}
