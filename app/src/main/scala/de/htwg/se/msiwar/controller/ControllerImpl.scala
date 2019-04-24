package de.htwg.se.msiwar.controller

import akka.actor.{Actor, ActorSystem, Props}
import akka.routing.RoundRobinPool
import de.htwg.se.msiwar.model._
import de.htwg.se.msiwar.util.Direction.Direction
import de.htwg.se.msiwar.util.{GameConfigProvider, GameConfigProviderImpl}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.event.Event
import scala.util.{Failure, Random, Success}

case class ControllerImpl(var model: GameModel) extends Controller {

  private val system = ActorSystem("GameGenerationSystem")

  override def cellContentToText(rowIndex: Int, columnIndex: Int): String = {
    model.cellContentToText(rowIndex, columnIndex)
  }

  override def cellContent(rowIndex: Int, columnIndex: Int): Option[GameObject] = {
    model.cellContent(rowIndex, columnIndex)
  }

  override def cellContentImagePath(rowIndex: Int, columnIndex: Int): Option[String] = {
    model.cellContentImagePath(rowIndex, columnIndex)
  }

  override def executeAction(actionId: Int, direction: Direction): Unit = {
    model.executeAction(actionId, direction).onComplete {
      case Success(action) => checkAfterActionExecution(action)
      case Failure(exception) => publish(Error(exception.getMessage))
    }
  }

  override def executeAction(actionId: Int, rowIndex: Int, columnIndex: Int): Unit = {
    model.executeAction(actionId, rowIndex, columnIndex).onComplete {
      case Success(action) => checkAfterActionExecution(action)
      case Failure(exception) => publish(Error(exception.getMessage))
    }
  }

  private def checkAfterActionExecution(actionResult: (GameModel, List[Event])) = {
    model = actionResult._1
    if (model.winnerId.isDefined) {
      publish(PlayerWon(model.winnerId.get, model.wonImagePath))
    }

    actionResult._2.foreach(publish(_))
    publish(PlayerStatsChanged(model.activePlayerNumber, model.activePlayerActionPoints))
    publish(TurnStarted(model.activePlayerNumber))
    cellsInRange(model.lastExecutedActionId)
  }

  override def cellsInRange(actionId: Option[Int]): Unit = {
    model.cellsInRange(actionId).onComplete {
      case Success(cells) => publish(CellsInRange(cells))
      case Failure(exception) => publish(Error(exception.getMessage))
    }
  }

  override def canExecuteAction(actionId: Int, direction: Direction): Future[Boolean] = {
    model.canExecuteAction(actionId, direction)
  }

  override def canExecuteAction(actionId: Int, rowIndex: Int, columnIndex: Int): Future[Boolean] = {
    model.canExecuteAction(actionId, rowIndex, columnIndex)
  }

  override def actionIds(playerNumber: Int): Set[Int] = {
    model.actionIdsForPlayer(playerNumber).get
  }

  override def actionDescription(actionId: Int): String = {
    model.actionDescription(actionId)
  }

  override def actionIconPath(actionId: Int): Option[String] = {
    model.actionIconPath(actionId)
  }

  override def scenarioIds: Set[Int] = {
    model.scenarioIds
  }

  override def scenarioName(scenarioId: Int): Option[String] = {
    model.scenarioName(scenarioId)
  }

  override def rowCount: Int = {
    model.rowCount
  }

  override def columnCount: Int = {
    model.columnCount
  }

  override def openingBackgroundImagePath: String = {
    model.gameConfigProvider.openingBackgroundImagePath
  }

  override def levelBackgroundImagePath: String = {
    model.gameConfigProvider.levelBackgroundImagePath
  }

  override def actionbarBackgroundImagePath: String = {
    model.gameConfigProvider.actionbarBackgroundImagePath
  }

  override def activePlayerNumber: Int = {
    model.activePlayerNumber
  }

  override def activePlayerName: String = {
    model.activePlayerName
  }

  override def startGame(scenarioId: Int): Unit = {
    model.startGame(scenarioId).onComplete {
      case Success(value) => {
        model = value
        publish(GameStarted())
        publish(TurnStarted(model.activePlayerNumber))
      }
      case Failure(exception) => publish(Error(exception.getMessage))
    }
  }

  override def turnCounter: Int = {
    model.turnCounter
  }

  override def actionPointCost(actionId: Int): Int = {
    model.actionPointCost(actionId)
  }

  override def actionDamage(actionId: Int): Int = {
    model.actionDamage(actionId)
  }

  override def actionRange(actionId: Int): Int = {
    model.actionRange(actionId)
  }

  override def activePlayerActionPoints: Int = {
    model.activePlayerActionPoints
  }

  override def activePlayerHealthPoints: Int = {
    model.activePlayerHealthPoints
  }

  override def appIconImagePath: String = {
    model.gameConfigProvider.appIconImagePath
  }

  override def startRandomGame(): Unit = {
    val gameGenActor = system.actorOf(Props(new GameGenerationActor(this)))
    gameGenActor ! Generate
  }

  override def startGame(gameConfigProviderOpt: Option[GameConfigProvider]): Unit = {
    gameConfigProviderOpt match {
      case Some(gameConfigProvider) => {
        model = model.init(gameConfigProvider)

        publish(GameStarted())
        publish(TurnStarted(model.activePlayerNumber))
      }
      case None => publish(ModelCouldNotGenerateGame())
    }
  }
}

case class GameGenerationActor(controller: Controller) extends Actor {
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

          controller.startGame(Option(newGameConfigProvider))
        }
        case None => controller.startGame(Option.empty)
      }
  }
}
