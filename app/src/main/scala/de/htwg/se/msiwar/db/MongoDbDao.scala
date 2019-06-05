package de.htwg.se.msiwar.db

import org.mongodb.scala._
import play.api.libs.json.{JsBoolean, JsValue, Json}

class MongoDbDao {
  val mongoClient: MongoClient = MongoClient()
  val database: MongoDatabase = mongoClient.getDatabase("pixeltankwars")
  val collection: MongoCollection[Document] = database.getCollection("gameconfigs")

  def getLatestSave(): JsValue = {
    var waitOnRes = true
    var res: JsValue = JsBoolean(true)
    val observable: Observable[Document] = collection.find().first()

    observable.subscribe(new Observer[Document] {
      override def onNext(result: Document): Unit = {
        res = Json.parse(result("config").toString)
      }
      override def onError(e: Throwable): Unit = println("Failed")
      override def onComplete(): Unit = {
        waitOnRes = false
        println("Completed")
      }
    })

    while(waitOnRes)
      Thread.sleep(10)

    res
  }

  def insert(gameConfig: GameConfig): Unit = {
    val observable: Observable[Completed] = collection.insertOne(Document( Json.obj("name" -> gameConfig.name, "config" -> gameConfig.config).toString()))

    observable.subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("Inserted")
      override def onError(e: Throwable): Unit = println("Failed")
      override def onComplete(): Unit = println("Completed")
    })
  }
}