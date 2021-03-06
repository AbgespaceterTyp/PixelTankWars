package de.htwg.se.msiwar.db

import org.mongodb.scala._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MongoDbGameConfigDao extends BaseDao {
  private val mongoClient: MongoClient = MongoClient()
  private val database: MongoDatabase = mongoClient.getDatabase("pixeltankwars")
  private val collection: MongoCollection[Document] = database.getCollection("gameconfigs")

  override def insert(gameConfig: GameConfig): Future[Int] = {
    Future {
      val docToInsert = Document(Json.obj("name" -> gameConfig.name, "config" -> gameConfig.config).toString())
      val observable: Observable[Completed] = collection.insertOne(docToInsert)

      observable.subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = println("Inserted")

        override def onError(e: Throwable): Unit = println("Failed")

        override def onComplete(): Unit = println("Completed")
      })
      1
    }
  }

  override def findAll: Future[Seq[Option[Int]]] = ???

  override def findById(id: Int): Future[GameConfig] = {
    Future {
      var waitOnRes = true
      var res = GameConfig("", "")
      val observable: Observable[Document] = collection.find().first

      observable.subscribe(new Observer[Document] {
        override def onNext(result: Document): Unit = {
          println("found result: " + result)
          val config = (Json.parse(result.toJson()) \ "config").get.as[String]
          val name = (Json.parse(result.toJson()) \ "name").get.as[String]
          res = GameConfig(name, config)
        }

        override def onError(e: Throwable): Unit = println("Failed")

        override def onComplete(): Unit = {
          waitOnRes = false
          println("Completed")
        }
      })

      while (waitOnRes) {
        Thread.sleep(10)
      }
      res
    }
  }

  override def update(id: Int, gameConfig: GameConfig): Future[Int] = ???

  override def delete(id: Int): Future[Int] = ???
}