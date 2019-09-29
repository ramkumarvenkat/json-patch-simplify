package com.jsonpatch.simplify

import org.specs2.mutable._
import org.specs2.specification.core.Fragments

class SimplifierSpec extends Specification with BaseSpec {

  val simplifier = new Simplifier()

  "Simplifier" should {
    val t = 1 to 8
    Fragments.foreach(t)(test)
  }

  def test(i: Int) =
    s"return the right results for $i.json" in {
      val testCase = getAsText(s"/input/$i.json")
      val expected = getAsJson(s"/output/$i.json")

      val result = simplifier.simplify(testCase)
      println(result)

      result mustEqual expected
    }
}
