package de.htwg.se.msiwar.model

import de.htwg.se.msiwar.model.ActionType._
import de.htwg.se.msiwar.util.Direction.Direction
import de.htwg.se.msiwar.util.{Direction, GameConfigProvider}

import scala.swing.event.Event
import scala.util.control.Breaks

case class GameModelImpl(gameConfigProvider: GameConfigProvider, gameBoard: GameBoard, lastExecutedAction: Option[Action], playerNumber: Int, turnNumber: Int) extends GameModel {

  override def init(gameConfigProvider: GameConfigProvider): GameModel = {
    copy(gameConfigProvider, GameBoard(gameConfigProvider.rowCount, gameConfigProvider.colCount, gameConfigProvider.gameObjects), Option.empty[Action], 1,1)
  }

  override def startGame(scenarioId: Int): GameModel = {
    val scenarioName = gameConfigProvider.listScenarios(scenarioId)
    val configProvider = gameConfigProvider.loadFromFile(scenarioName)
    init(configProvider)
  }

  override def activePlayerName: String = {
    gameBoard.player(playerNumber) match {
      case Some(value) => value.name
      case None => ""
    }
  }

  override def actionIdsForPlayer(playerNumber: Int): Option[Set[Int]] = {
    player(playerNumber).map(_.actions.map(_.id).toSet)
  }

  override def actionPointCost(actionId: Int): Int = {
    actions.find(_.id == actionId) match {
      case Some(value) => value.actionPoints
      case None => 0
    }
  }

  override def actionDescription(actionId: Int): String = {
    actions.find(_.id == actionId) match {
      case Some(value) => value.description
      case None => ""
    }
  }

  private def actions: Set[Action] = {
    gameBoard.players.flatMap(_.actions).toSet
  }

  override def actionIconPath(actionId: Int): Option[String] = {
    actions.find(_.id == actionId) match {
      case Some(value) => Option(value.imagePath)
      case None => Option.empty
    }
  }

  override def executeAction(actionId: Int, rowIndex: Int, columnIndex: Int): (GameModel, List[Event]) = {
    gameBoard.player(playerNumber) match {
      case Some(player) => executeAction(actionId, gameBoard.calculateDirection(player.position, Position(rowIndex, columnIndex)))
      case None => (this, List())
    }
  }

  override def executeAction(actionId: Int, direction: Direction): (GameModel, List[Event]) = {
    gameBoard.player(playerNumber) match {
      case Some(activePlayer) => {
        activePlayer.actions.find(_.id == actionId) match {
          case Some(actionToExecute) => {
            var newGameBoard: GameBoard = gameBoard.copy()
            var events: List[Event] = List[Event]()
            // Update view direction first to ensure correct view direction on action execution
            val newActivePlayer = activePlayer.copy(viewDirection=direction)
            newGameBoard = newGameBoard.placeGameObject(newActivePlayer)

            actionToExecute.actionType match {
              case MOVE =>
                newGameBoard.calculatePositionForDirection(activePlayer.position, direction, actionToExecute.range) match {
                  case Some(newPosition) => {
                    val oldPosition = newActivePlayer.position
                    newGameBoard = newGameBoard.moveGameObject(newActivePlayer, newPosition)
                    events = events.:+(CellChanged(List((newPosition.rowIdx, newPosition.columnIdx), (oldPosition.rowIdx, oldPosition.columnIdx))))
                  }
                  case None => // Do nothing - player cannot move to target position
                }
              case SHOOT =>
                val shootResult = executeShoot(newActivePlayer, newGameBoard, actionToExecute, direction)
                newGameBoard = shootResult._1
                events = shootResult._2:::events
              case WAIT => // Do nothing
            }
            newGameBoard = updateActionPoints(newGameBoard, activePlayerNumber, actionToExecute)

            val nextTurn = updateTurn(Option(actionToExecute), newGameBoard)
            // Reset player actions points when turn changed
            if(nextTurn._2 != turnNumber){
              newGameBoard = resetPlayerActionPoints(newGameBoard, newGameBoard.players)
            }
            (copy(gameConfigProvider, newGameBoard, Option(actionToExecute), nextTurn._1, nextTurn._2), events)
          }
          case None => (this, List[Event]())
        }
      }
      case None => (this, List[Event]())
    }
  }

  private def resetPlayerActionPoints(gameBoard: GameBoard, players: List[PlayerObject]): GameBoard = {
    if(!players.isEmpty){
      val player = players.head
      val newGameBoard = gameBoard.placeGameObject(player.copy(actionPoints = player.maxActionPoints))
      resetPlayerActionPoints(newGameBoard, players.tail)
    } else {
      gameBoard
    }
  }

  private def updateActionPoints(gameBoard: GameBoard, playerNumber: Int, action: Action) : GameBoard = {
    gameBoard.player(playerNumber) match {
      case Some(player) => {
        val newActionPoints = player.actionPoints - action.actionPoints
        gameBoard.placeGameObject(player.copy(actionPoints = newActionPoints))
      }
      case None => gameBoard
    }
  }

  private def executeShoot(player: PlayerObject, gameBoard: GameBoard, shootAction: Action, direction: Direction): (GameBoard, List[Event]) = {
    gameBoard.calculatePositionForDirection(player.position, direction, shootAction.range) match {
      case Some(positionForDirection) => {
        gameBoard.collisionObject(player.position, positionForDirection, ignoreLastPosition = false) match {
          case Some(collisionObject) => playerOrBlockHit(collisionObject,gameBoard,shootAction)
          case None => (gameBoard, nothingHit(gameBoard, player.position, direction, shootAction))
        }
      }
      case None => (gameBoard, List())
    }
  }

  private def playerOrBlockHit(collisionObject : GameObject, gameBoard: GameBoard, shootAction: Action) : (GameBoard, List[Event]) = {
    collisionObject match {
      case playerObjectHit: PlayerObject =>
        val newGameBoard = damageAndRemoveDeadPlayer(playerObjectHit, gameBoard,  shootAction)
        val events = List(CellChanged(List((playerObjectHit.position.rowIdx, playerObjectHit.position.columnIdx))),
          AttackResult(collisionObject.position.rowIdx, collisionObject.position.columnIdx, hit = true, gameConfigProvider.attackImagePath, gameConfigProvider.attackSoundPath))
        (newGameBoard, events)
      case blockObject: BlockObject => {
        val events = List(CellChanged(List((blockObject.position.rowIdx, blockObject.position.columnIdx))),
          AttackResult(collisionObject.position.rowIdx, collisionObject.position.columnIdx, hit = true, gameConfigProvider.attackImagePath, gameConfigProvider.attackSoundPath))
        (gameBoard, events)
      }
    }
  }

  private def nothingHit(gameBoard: GameBoard, startingPosition: Position, viewDirection: Direction, shootAction: Action): List[Event] = {
    gameBoard.calculatePositionForDirection(startingPosition, viewDirection, shootAction.range) match {
      case Some(targetPosition) => List(CellChanged(List((startingPosition.rowIdx, startingPosition.columnIdx))),
        AttackResult(targetPosition.rowIdx, targetPosition.columnIdx, hit = false, gameConfigProvider.attackImagePath, gameConfigProvider.attackSoundPath))
      case None => List()
    }
  }

  private def damageAndRemoveDeadPlayer(playerObject: PlayerObject, gameBoard: GameBoard, shootAction: Action) : GameBoard = {
    val updatedPlayerObject = playerObject.copy(healthPoints = playerObject.healthPoints - shootAction.damage)
    if (updatedPlayerObject.healthPoints <= 0) {
      gameBoard.removeGameObject(updatedPlayerObject)
    } else {
      gameBoard.placeGameObject(updatedPlayerObject)
    }
  }

  private def updateTurn(lastAction: Option[Action], currentGameBoard: GameBoard): (Int, Int) = {
    val currentPlayerOpt = currentGameBoard.player(playerNumber)
    if (currentPlayerOpt.isDefined && currentPlayerOpt.get.actionPoints <= 0) {
      val nextPlayerOpt = currentGameBoard.players.find(_.playerNumber > activePlayerNumber)
      // If every player did his turn, start the next turn with first player alive
      if (!nextPlayerOpt.isDefined) {
        val nextTurnNumber = turnCounter +  1
        // Set next player to first player found which is alive
        val nextPlayer =  currentGameBoard.players.reduceLeft((a, b) => if (a.playerNumber < b.playerNumber) a else b)
        (nextPlayer.playerNumber, nextTurnNumber)
      } else {
        (nextPlayerOpt.get.playerNumber, turnCounter)
      }
    } else {
      (playerNumber, turnNumber)
    }
  }

  override def lastExecutedActionId: Option[Int] = {
    lastExecutedAction match {
      case Some(value) => Option(value.id)
      case None => Option.empty[Int]
    }
  }

  override def canExecuteAction(actionId: Int, rowIndex: Int, columnIndex: Int): Boolean = {
    canExecuteAction(actionId, gameBoard.calculateDirection(gameBoard.player(playerNumber).get.position, Position(rowIndex, columnIndex)))
  }

  override def canExecuteAction(actionId: Int, direction: Direction): Boolean = {
    winnerId match {
      case Some(_) => false
      case None => checkActionExecution(actionId, direction)
    }
  }

  private def checkActionExecution(actionId: Int, direction: Direction): Boolean = {
    gameBoard.player(playerNumber).get.actions.find(_.id == actionId) match {
      case Some(value) => {
        value.actionType match {
          case MOVE => {
            val newPositionOpt = gameBoard.calculatePositionForDirection(gameBoard.player(playerNumber).get.position, direction, value.range)
            newPositionOpt match {
              case Some(_) => {
                val newPosition = newPositionOpt.get
                gameBoard.isInBound(newPosition) && gameBoard.gameObjectAt(newPosition).isEmpty
              }
              // Any other actions are always allowed
              case None => false
            }
          }
          // All other actions are always allowed
          case _ => true
        }
      }
      // No player found -> do not allow
      case None => false
    }
  }

  override def winnerId: Option[Int] = {
    val playersAliveIds = gameBoard.players.filter(_.healthPoints > 0).map(_.playerNumber)
    playersAliveIds.lengthCompare(1) match {
      case 0 => Option(playersAliveIds.head)
      case _ => Option.empty
    }
  }

  private def player(playerNumber: Int): Option[PlayerObject] = {
    gameBoard.players.find(_.playerNumber == playerNumber)
  }

  override def activePlayerNumber: Int = {
    playerNumber
  }

  override def turnCounter: Int = turnNumber

  override def cellContentImagePath(rowIndex: Int, columnIndex: Int): Option[String] = {
    gameBoard.gameObjectAt(rowIndex, columnIndex) match {
      case Some(value) => {
        value match {
          case playerObj: PlayerObject => Option(imagePathForViewDirection(playerObj.imagePath, playerObj.viewDirection))
          case blockObj: BlockObject => Option(blockObj.imagePath)
        }
      }
      case None => Option.empty[String]
    }
  }

  private def imagePathForViewDirection(imagePath: String, viewDirection: Direction): String = {
    val basePath = imagePath.substring(0, imagePath.lastIndexOf('.'))
    val imageExtension = imagePath.substring(imagePath.lastIndexOf('.'), imagePath.length)

    val sb = StringBuilder.newBuilder
    sb.append(basePath)
    sb.append("_")
    sb.append(Direction.toDegree(viewDirection))
    sb.append(imageExtension)
    sb.toString()
  }

  override def cellContentToText(rowIndex: Int, columnIndex: Int): String = {
    gameBoard.gameObjectAt(rowIndex, columnIndex) match {
      case Some(value) => {
        value match {
          case playerObj: PlayerObject => playerObj.playerNumber.toString
          case blockObj: BlockObject => blockObj.name
        }
      }
      case None => "X"
    }
  }

  override def cellContent(rowIndex: Int, columnIndex: Int) : Option[GameObject] = {
    gameBoard.gameObjectAt(rowIndex, columnIndex)
  }

  override def cellsInRange(actionId: Option[Int]): List[(Int, Int)] = {
    actionId match {
      case Some(value) => {
        gameBoard.player(playerNumber).get.actions.find(_.id == value) match {
          case Some(value) =>  gameBoard.reachableCells(gameBoard.player(playerNumber).get.position, value)
          case None => List()
        }
      }
      case None => List()
    }
  }

  override def rowCount: Int = {
    gameBoard.rows
  }

  override def columnCount: Int = {
    gameBoard.columns
  }

  override def actionDamage(actionId: Int): Int = {
    gameBoard.player(playerNumber).get.actions.find(_.id == actionId) match {
      case Some(value) => value.damage
      case None => 0
    }
  }

  override def actionRange(actionId: Int): Int = {
    gameBoard.player(playerNumber).get.actions.find(_.id == actionId) match {
      case Some(value) => value.range
      case None => 0
    }
  }

  override def wonImagePath: String = {
    winnerId match {
        // TODO replace get with match
      case Some(value) => player(value).get.wonImagePath
      case None => ""
    }
  }

  override def scenarioIds: Set[Int] = {
    gameConfigProvider.listScenarios.indices.toSet
  }

  override def scenarioName(scenarioId: Int): Option[String] = {
    if(scenarioId >= 0 && scenarioId < gameConfigProvider.listScenarios.size) {
      val scenarioName = gameConfigProvider.listScenarios(scenarioId)
      Option(scenarioName.substring(0, scenarioName.lastIndexOf('.')).replace('_', ' '))
    } else {
      Option.empty
    }
  }

  override def activePlayerActionPoints: Int = {
    gameBoard.player(playerNumber) match {
      case Some(value) => value.actionPoints
      case None => 0
    }
  }

  override def activePlayerHealthPoints: Int = {
    gameBoard.player(playerNumber) match {
      case Some(value) => value.healthPoints
      case None => 0
    }
  }
}
