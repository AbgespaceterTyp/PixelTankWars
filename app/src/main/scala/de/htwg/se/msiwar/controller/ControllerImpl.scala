package de.htwg.se.msiwar.controller

import de.htwg.se.msiwar.model._
import de.htwg.se.msiwar.util.Direction.Direction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.event.Event
import scala.util.{Failure, Success}

case class ControllerImpl(var model: GameModel) extends Controller {

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
    model = model.startRandomGame()

    publish(GameStarted())
    publish(TurnStarted(model.activePlayerNumber))
  }
}
