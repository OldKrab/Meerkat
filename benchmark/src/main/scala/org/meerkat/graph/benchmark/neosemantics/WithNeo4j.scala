package org.meerkat.graph.benchmark.neosemantics

import java.io.File
import java.util

import org.meerkat.Syntax.syn
import org.meerkat.graph.neo4j.Neo4jInput
import org.meerkat.graph.neo4j.Neo4jInput.Entity
import org.meerkat.parsers.Parsers._
import org.meerkat.parsers._
import org.neo4j.graphdb.factory.{GraphDatabaseFactory, GraphDatabaseSettings}

trait WithNeo4j extends App with SimpleBenchmark {
  def withGraph(pathToFile: String, pathToConf: String)(
      fun: (Neo4jInput => Unit)): Unit = {

    val dbFile = new File(pathToFile)
    val dbConf = new File(pathToConf)
    val graphDb = new GraphDatabaseFactory()
      .newEmbeddedDatabaseBuilder(dbFile)
      .loadPropertiesFromFile(dbConf.getAbsolutePath)
      .setConfig(GraphDatabaseSettings.allow_upgrade, "true")
      .newGraphDatabase()

    val tx    = graphDb.beginTx()
    val graph = new Neo4jInput(graphDb)

    fun(graph)

    tx.success()
    tx.close()
  }
}

object RdfConstants {
  val RDFS__SUB_CLASS_OF       = "rdfs__subClassOf"
  val RDF__TYPE                = "rdf__type"
  val SKOS__BROADER_TRANSITIVE = "skos__broaderTransitive"
}

sealed trait QueryToDb[L, N, V] {
  def checkIfHas[T](e: Entity, prop: String)(p: T => Boolean): Boolean = {
    e.hasProperty(prop) && p(e.getProperty[T](prop))
  }

  def startVertexes: Symbol[L, Entity, Entity] =
    syn(V(checkIfHas[N](_: Entity, "uri")(_ => true)) ^^)
  def findFinishQueryWithoutStart: Symbol[L, N, V]
  def findPathsQueryWithoutStart: Symbol[L, N, util.ArrayDeque[V]]
  def findPathLengthQueryWithoutStart: Symbol[L, N, Int]

}

sealed trait RdfQuery {
  protected val uriFromV: Symbol[Entity, Entity, String] =
    syn(V((_: Entity) => true) ^ ((_: Entity).getProperty[String]("uri")))
}

case class SameGeneration(private val edgeName: String)
    extends QueryToDb[Entity, Entity, String]
    with RdfQuery {
  protected val OUT: Edge[Entity] = outE((_: Entity).label() == edgeName)
  protected val IN: Edge[Entity]  = inE((_: Entity).label() == edgeName)

  override def startVertexes: Symbol[Entity, Entity, Entity] =
    syn(OUT ~ OUT ~ OUT ~ OUT ~ super.startVertexes ~ OUT &&)

  private def S: Symbol[Entity, Entity, _] = syn(IN ~ S ~ OUT | OUT)
  override def findFinishQueryWithoutStart: Symbol[Entity, Entity, String] =
    syn(
      S.?
      // without & {case ...} type error
        ~ uriFromV
        & { case _ ~ a => a }
    )

  override def findPathsQueryWithoutStart
    : Symbol[Entity, Entity, util.ArrayDeque[String]] = syn(
    syn(
      IN ~ uriFromV ~ findPathsQueryWithoutStart ~ OUT ~ uriFromV
        & {
          case (l: String) ~ (deque: util.ArrayDeque[String]) ~ (r: String) =>
            deque.addLast(r)
            deque.addFirst(l)
            deque
        }
        | OUT ~ uriFromV
          & {
            case uri: String =>
              val deque = new util.ArrayDeque[String]()
              deque.add(uri)
              deque
          }
    )
  )
  override def findPathLengthQueryWithoutStart: Symbol[Entity, Entity, Int] =
    syn(IN ~ findPathLengthQueryWithoutStart ~ OUT & {
      case (inLen: Int) => inLen + 2
    } | OUT ~ uriFromV & (_ => 1))
}

case object GPPerf1 extends QueryToDb[Entity, Entity, String] with RdfQuery {

  protected val SCO: Edge[Entity] = outE(
    (_: Entity).label() == RdfConstants.RDFS__SUB_CLASS_OF)
  protected val SCOR: Edge[Entity] = inE(
    (_: Entity).label() == RdfConstants.RDFS__SUB_CLASS_OF)

  protected val T: Edge[Entity] = outE(
    (_: Entity).label() == RdfConstants.RDF__TYPE)
  protected val TR: Edge[Entity] = inE(
    (_: Entity).label() == RdfConstants.RDF__TYPE)

  private def S: Symbol[Entity, Entity, _] =
    syn(SCOR ~ S ~ SCO | TR ~ S ~ T | SCOR ~ SCO | TR ~ T)

  override def findFinishQueryWithoutStart: Symbol[Entity, Entity, String] =
    syn(
      S.?
      // without & {case ...} type error
        ~ syn(V((_: Entity) => true) ^ ((_: Entity).getProperty[String]("uri")))
        & { case _ ~ a => a }
    )
  override def findPathsQueryWithoutStart
    : Symbol[Entity, Entity, util.ArrayDeque[String]] = ???

  override def findPathLengthQueryWithoutStart: Symbol[Entity, Entity, Int] =
    syn(
      SCOR ~ findPathLengthQueryWithoutStart ~ SCO & {
        case inLen: Int => inLen + 2
      }
        | TR ~ findPathLengthQueryWithoutStart ~ T & {
          case inLen: Int => inLen + 2
        }
        | SCOR ~ uriFromV ~ SCO & (_ => 2)
        | TR ~ uriFromV ~ T & (_ => 2)
    )
}
