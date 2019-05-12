package de.htwg.ptw.generator

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import de.htwg.ptw.common.util.GameConfigProviderImpl
import de.htwg.ptw.generator.util.JsonConverter

import scala.concurrent.Future
import scala.util.{Failure, Success}

class GameGeneratorApp() {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  def generate(rowCount: Int, colCount: Int): Unit = {
    val gameGenActor = system.actorOf(Props(new GameGenerationActor()))
    gameGenActor ! Generate(rowCount, colCount)
  }
}

class GameGenerationActor() extends Actor {
  private val workerRouter = context.actorOf(Props[GameGenerationWorker].withRouter(RoundRobinPool(10)), name = "workerRouter")
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  def receive: PartialFunction[Any, Unit] = {
    case Generate(rowCount: Int, colCount: Int) =>
      for (_ <- 0 until 10) {
        workerRouter ! Work(rowCount, colCount)
      }
    case Result(gameObjectsOpt, genRowCount, genColCount) =>
      gameObjectsOpt match {
        case Some(gameObjects) => {
          context.stop(self)

          val newGameConfigProvider = GameConfigProviderImpl(gameObjects, "sounds/explosion.wav", "images/background_opening.png",
            "images/background_woodlands.png", "images/background_actionbar.png", "images/hit.png",
            "images/app_icon.png", genRowCount, genColCount)

          val data = JsonConverter.gameConfigProvider.writes(newGameConfigProvider).toString()
          print("sending data: " + data)
          val request = HttpRequest(
            PUT,
            uri = "http://localhost:8080/start",
            entity = HttpEntity(`application/json`, data))
          val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

          responseFuture
            .onComplete {
              case Success(res) => println(res)
              case Failure(restError) => sys.error("Failed to send game configuration: " + restError)
            }
        }
        case None => {
          // Send empty result when no valid game configuration has been generated
          val request = HttpRequest(
            PUT,
            uri = "http://localhost:8080/start",
            entity = HttpEntity(`application/json`, ""))
          val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

          responseFuture
            .onComplete {
              case Success(res) => println(res)
              case Failure(restError) => sys.error("Failed to send empty game configuration: " + restError)
            }
        }
      }
  }
}
