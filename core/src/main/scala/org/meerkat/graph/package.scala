package org.meerkat

import org.meerkat.input.{GraphxInput, Input}
import org.meerkat.parsers.{AbstractCPSParsers, Trampoline}
import org.meerkat.sppf.{DefaultSPPFLookup, NonterminalNode}
import org.meerkat.util._

import scala.language.reflectiveCalls
import scalax.collection.Graph
import scalax.collection.edge.Implicits._

package object graph {
  def parseGraphFromAllPositions[L, N](parser: AbstractCPSParsers.AbstractSymbol[L, N,_, _],
                                 graph: Input[L, N],
                                 nontermsOpt: Option[List[String]] = None): collection.Seq[NonterminalNode] = {
    val sppfLookup = new DefaultSPPFLookup[L](graph)
    val nodesCount = graph.edgesCount
    parser.reset()
    for (i <- 0 until nodesCount) {
      parser(graph, i, sppfLookup)(t => {})
      Trampoline.run
    }
    nontermsOpt
      .getOrElse(List(parser.name))
      .flatMap(sppfLookup.findNonterminalsByName)
  }

  def edgesToInMemoryGraph(edges: List[(Int, String, Int)], nodesCount: Int): GraphxInput[String] = {
    val scalaxEdges = edges.map {
      case (f, l, t) =>
        (f ~+#> t)(l)
    }
    GraphxInput(Graph(scalaxEdges: _*))
  }
}

