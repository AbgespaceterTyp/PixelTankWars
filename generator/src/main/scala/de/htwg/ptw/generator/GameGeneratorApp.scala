package de.htwg.ptw.generator

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import de.htwg.ptw.common.util.GameConfigProviderImpl

import scala.concurrent.{ExecutionContext, Future}
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

          val request = HttpRequest(
            PUT,
            uri = "http://localhost:8080/start",
            entity = HttpEntity(`application/json`, Marshal(newGameConfigProvider).toString))
          val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

          responseFuture
            .onComplete {
              case Success(res) => println(res)
              case Failure(restError) => sys.error("Failed to start random game: " + restError)
            }
          //controller.startGame(Option(newGameConfigProvider))
        }
        case None => // TODO do rest call with empty game config
        //controller.startGame(Option.empty)
      }
  }
}
