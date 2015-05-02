package org.meerkat.parser

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.meerkat.meerkat._
import org.meerkat.util.Visualization
import org.meerkat.sppf.FlattenSPPF

@RunWith(classOf[JUnitRunner])
class FlatteningTest extends FunSuite {

  test("test1") {
    val A: MeerkatParser = "A" ::= "a"
    val B: MeerkatParser = "B" ::= "b"
    val C: MeerkatParser = "C" ::= "c"
    val D: MeerkatParser = "D" ::= "d"
    val E: MeerkatParser = "E" ::= "e"
    val S: MeerkatParser = "S" ::= A ~~ B ~~ C ~~ D ~~ E
    
    new MeerkatParsers {} . parse(S, "abcde")
    
  }
  
}

object FlatteningTest {
  
  def main(args: Array[String]): Unit = {
    val A: MeerkatParser = "A" ::= "a"
    val B: MeerkatParser = "B" ::= "b"
    val C: MeerkatParser = "C" ::= "c"
    val D: MeerkatParser = "D" ::= "d"
    val E: MeerkatParser = "E" ::= "e"
    val S: MeerkatParser = "S" ::= A ~~ B ~~ C ~~ D ~~ E
    
    val r = new MeerkatParsers {} . parse(S, "abcde")
    r match {
      case ParseSuccess(r, s) => FlattenSPPF.flatten(r) //Visualization.toDot(r) 
      case _               => println("Parse error")
    }
    
  }  
}