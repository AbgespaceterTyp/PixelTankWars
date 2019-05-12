package de.htwg.ptw.generator

import akka.actor.{Actor, ActorSystem, Props}
import akka.routing.RoundRobinPool
import de.htwg.ptw.common.util.GameConfigProviderImpl

import scala.util.Random

class GameGeneratorApp() {
  private val system = ActorSystem("GameGenerationSystem")

  def generate(rowCount : Int, colCount : Int): Unit = {
    val gameGenActor = system.actorOf(Props(new GameGenerationActor()))
    gameGenActor ! Generate
  }
}

class GameGenerationActor() extends Actor {
  private val workerRouter = context.actorOf(Props[GameGenerationWorker].withRouter(RoundRobinPool(10)), name = "workerRouter")

  def receive: PartialFunction[Any, Unit] = {
    case Generate =>
      for (_ <- 0 until 10) {
        workerRouter ! Work(Random.nextInt(10) + 2, Random.nextInt(20) + 2)
      }
    case Result(gameObjectsOpt, genRowCount, genColCount) =>
      gameObjectsOpt match {
        case Some(gameObjects) => {
          context.stop(self)

          val newGameConfigProvider = GameConfigProviderImpl(gameObjects, "sounds/explosion.wav", "images/background_opening.png",
            "images/background_woodlands.png", "images/background_actionbar.png", "images/hit.png",
            "images/app_icon.png", genRowCount, genColCount)

          // TODO do rest call
          //controller.startGame(Option(newGameConfigProvider))
        }
        case None => // TODO do rest call with empty game config
        //controller.startGame(Option.empty)
      }
  }
}
