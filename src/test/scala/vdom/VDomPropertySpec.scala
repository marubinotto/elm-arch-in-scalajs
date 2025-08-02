package vdom

import org.scalatest.propspec.AnyPropSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.{Arbitrary, Gen}
import cats.effect.IO

class VDomPropertySpec
    extends AnyPropSpec
    with Matchers
    with ScalaCheckPropertyChecks {

  // Generators for VNode
  implicit val arbVNode: Arbitrary[VNode] = Arbitrary(genVNode(3))

  def genVNode(maxDepth: Int): Gen[VNode] = {
    if (maxDepth <= 0) {
      genVText
    } else {
      Gen.oneOf(genVText, genVElement(maxDepth - 1))
    }
  }

  def genVText: Gen[VText] = {
    Gen.alphaNumStr.map(VText.apply)
  }

  def genVElement(maxDepth: Int): Gen[VElement] = {
    for {
      tag <- Gen.oneOf("div", "span", "p", "h1", "button", "input")
      attrs <- genAttrs
      events <- genEvents
      children <- Gen.listOfN(
        Gen.choose(0, 3).sample.getOrElse(0),
        genVNode(maxDepth)
      )
    } yield VElement(tag, attrs, events, children)
  }

  def genAttrs: Gen[Map[String, String]] = {
    Gen.mapOfN(
      Gen.choose(0, 3).sample.getOrElse(0),
      for {
        key <- Gen.oneOf("class", "id", "data-test", "style")
        value <- Gen.alphaNumStr
      } yield key -> value
    )
  }

  def genEvents: Gen[Map[String, IO[Unit]]] = {
    Gen.mapOfN(
      Gen.choose(0, 2).sample.getOrElse(0),
      for {
        event <- Gen.oneOf("click", "hover", "focus", "blur")
      } yield event -> IO.unit
    )
  }

  property(
    "diff should be idempotent - diffing identical nodes returns empty patches"
  ) {
    forAll { (node: VNode) =>
      val patches = VDom.diff(node, node)
      patches shouldBe empty
    }
  }

  property("diff should handle any two nodes without throwing exceptions") {
    forAll { (oldNode: VNode, newNode: VNode) =>
      noException should be thrownBy VDom.diff(oldNode, newNode)
    }
  }

  property(
    "replacing a node should always result in a single Replace patch when nodes are completely different"
  ) {
    forAll { (text: String, tag: String) =>
      whenever(text.nonEmpty && tag.nonEmpty) {
        val textNode = VText(text)
        val elementNode = VElement(tag, Map.empty, Map.empty, List.empty)

        val patches1 = VDom.diff(textNode, elementNode)
        val patches2 = VDom.diff(elementNode, textNode)

        patches1 should contain only Replace(elementNode)
        patches2 should contain only Replace(textNode)
      }
    }
  }

  property("text node updates should only produce UpdateText patches") {
    forAll { (oldText: String, newText: String) =>
      whenever(oldText != newText) {
        val oldNode = VText(oldText)
        val newNode = VText(newText)
        val patches = VDom.diff(oldNode, newNode)

        patches should have length 1
        patches.head shouldBe UpdateText(newText)
      }
    }
  }

  property(
    "elements with same tag but different attributes should produce UpdateAttrs patches"
  ) {
    forAll {
      (
          tag: String,
          oldAttrs: Map[String, String],
          newAttrs: Map[String, String]
      ) =>
        whenever(tag.nonEmpty && oldAttrs != newAttrs) {
          val oldNode = VElement(tag, oldAttrs, Map.empty, List.empty)
          val newNode = VElement(tag, newAttrs, Map.empty, List.empty)
          val patches = VDom.diff(oldNode, newNode)

          patches should not be empty
          patches.head shouldBe a[UpdateAttrs]
        }
    }
  }

  property(
    "diff should be consistent - same inputs should produce same outputs"
  ) {
    forAll { (oldNode: VNode, newNode: VNode) =>
      val patches1 = VDom.diff(oldNode, newNode)
      val patches2 = VDom.diff(oldNode, newNode)
      patches1 shouldEqual patches2
    }
  }

  property("empty children lists should not produce child patches") {
    forAll { (tag: String, attrs: Map[String, String]) =>
      whenever(tag.nonEmpty) {
        val oldNode = VElement(tag, attrs, Map.empty, List.empty)
        val newNode = VElement(tag, attrs, Map.empty, List.empty)
        val patches = VDom.diff(oldNode, newNode)

        patches shouldBe empty
      }
    }
  }

  property("adding children should produce InsertChild patches") {
    forAll { (tag: String, child: VNode) =>
      whenever(tag.nonEmpty) {
        val oldNode = VElement(tag, Map.empty, Map.empty, List.empty)
        val newNode = VElement(tag, Map.empty, Map.empty, List(child))
        val patches = VDom.diff(oldNode, newNode)

        patches should contain(InsertChild(0, child))
      }
    }
  }

  property("removing all children should produce RemoveChild patches") {
    forAll { (tag: String, children: List[VNode]) =>
      whenever(tag.nonEmpty && children.nonEmpty) {
        val oldNode = VElement(tag, Map.empty, Map.empty, children)
        val newNode = VElement(tag, Map.empty, Map.empty, List.empty)
        val patches = VDom.diff(oldNode, newNode)

        // Should have RemoveChild patches for each child (in reverse order)
        val removePatches = patches.collect { case r: RemoveChild => r }
        removePatches should have length children.length
      }
    }
  }
}
