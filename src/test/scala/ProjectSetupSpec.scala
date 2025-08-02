import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.should.Matchers._
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.softwaremill.quicklens._

class ProjectSetupSpec extends AsyncFunSpec with AsyncIOSpec {

  describe("Project Setup") {
    it("should have Cats Effect working") {
      IO.pure("Hello, Cats Effect!").asserting(_ shouldBe "Hello, Cats Effect!")
    }

    it("should have QuickLens working") {
      case class TestModel(name: String, count: Int)
      val model = TestModel("test", 0)
      val updated = model.modify(_.count).using(_ + 1)
      IO.pure(updated.count).asserting(_ shouldBe 1)
    }

    it("should have basic functionality working") {
      val list = List(1, 2, 3)
      IO.pure(list).asserting { l =>
        l should have size 3
        l should contain(2)
      }
    }
  }
}
