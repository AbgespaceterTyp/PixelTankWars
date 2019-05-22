package de.htwg.ptw.generator

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, HttpResponse}
import akka.routing.RoundRobinPool
import akka.stream.{ActorMaterializer, Materializer}
import de.htwg.ptw.common.util.{BaseJsonConverter, GameConfigProviderImpl}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class GameGeneratorApp() {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  def generate(rowCount: Int, colCount: Int): Unit = {
    val gameGenActor = system.actorOf(Props(new GameGenerationActor()))
    gameGenActor ! Generate(rowCount, colCount)
  }
}

class GameGenerationActor(implicit system: ActorSystem, implicit val mat: Materializer, implicit val actorContext: ExecutionContextExecutor) extends Actor {
  private val workerRouter = context.actorOf(Props[GameGenerationWorker].withRouter(RoundRobinPool(10)), name = "workerRouter")
  private val jsonConverter = new BaseJsonConverter

  def receive: PartialFunction[Any, Unit] = {
    case Generate(rowCount: Int, colCount: Int) =>
      for (_ <- 0 until 10) {
        workerRouter ! Work(rowCount, colCount)
      }
    case Result(gameObjectsOpt, genRowCount, genColCount, backgroundPath) =>
      gameObjectsOpt match {
        case Some(gameObjects) => {
          context.stop(self)

          val newGameConfigProvider = GameConfigProviderImpl(gameObjects, "sounds/explosion.wav", "images/background_opening.png",
            backgroundPath, "images/background_actionbar.png", "images/hit.png",
            "images/app_icon.png", genRowCount, genColCount)

          val data = jsonConverter.gameConfigProviderWriter.writes(newGameConfigProvider).toString()
          println("Sending data: " + data)
          sendJson(data)
        }
        case None => println("No valid configuration has been generated")
      }
  }

  def sendJson(data: String): Unit = {
    val request = HttpRequest(
      PUT,
      uri = "http://app:8080/start",
      entity = HttpEntity(`application/json`, data))
    val responseFuture: Future[HttpResponse] = Http().singleRequest(request)

    responseFuture
      .onComplete {
        case Success(res) => println("Result: " + res)
        case Failure(restError) => sys.error("Failed to send game configuration: " + restError)
      }
  }
}
