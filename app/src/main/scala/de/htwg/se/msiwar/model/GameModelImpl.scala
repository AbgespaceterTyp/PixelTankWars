package de.htwg.se.msiwar.model

import de.htwg.ptw.common.ActionType._
import de.htwg.ptw.common.Direction.Direction
import de.htwg.ptw.common.model._
import de.htwg.ptw.common.util.{GameConfigProvider, GameConfigProviderImpl}
import de.htwg.ptw.common.{Direction, model}
import de.htwg.se.msiwar.db.{GameConfig, SlickGameConfigDao, MongoDbGameConfigDao}
import de.htwg.se.msiwar.util.JsonConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.swing.event.Event

case class GameModelImpl(gameConfigProvider: GameConfigProvider, gameBoard: GameBoard, lastExecutedAction: Option[Action], playerNumber: Int, turnNumber: Int) extends GameModel {

  override def init(gameConfigProvider: GameConfigProvider): GameModel = {
    copy(gameConfigProvider, model.GameBoard(gameConfigProvider.rowCount, gameConfigProvider.colCount, gameConfigProvider.gameObjects), Option.empty[Action], 1, 1)
  }

  override def startGame(scenarioId: Int): Future[GameModel] = {
    Future {
      val scenarioName = gameConfigProvider.listScenarios(scenarioId)
      val configProvider = gameConfigProvider.loadFromFile(scenarioName)
      init(configProvider)
    }
  }

  override def activePlayerName: String = {
    gameBoard.player(playerNumber) match {
      case Some(player) => player.name
      case None => ""
    }
  }

  override def actionIdsForPlayer(playerNumber: Int): Option[Set[Int]] = {
    player(playerNumber).map(_.actions.map(_.id).toSet)
  }

  override def actionPointCost(actionId: Int): Int = {
    actions.find(_.id == actionId) match {
      case Some(action) => action.actionPoints
      case None => 0
    }
  }

  override def actionDescription(actionId: Int): String = {
    actions.find(_.id == actionId) match {
      case Some(action) => action.description
      case None => ""
    }
  }

  private def actions: Set[Action] = {
    gameBoard.players.flatMap(_.actions).toSet
  }

  override def actionIconPath(actionId: Int): Option[String] = {
    actions.find(_.id == actionId) match {
      case Some(action) => Option(action.imagePath)
      case None => Option.empty
    }
  }

  override def executeAction(actionId: Int, rowIndex: Int, columnIndex: Int): Future[(GameModel, List[Event])] = {
    gameBoard.player(playerNumber) match {
      case Some(player) => executeAction(actionId, gameBoard.calculateDirection(player.position, Position(rowIndex, columnIndex)))
      case None => Future((this, List()))
    }
  }

  override def executeAction(actionId: Int, direction: Direction): Future[(GameModel, List[Event])] = {
    Future {
      gameBoard.player(playerNumber) match {
        case Some(activePlayer) => {
          activePlayer.actions.find(_.id == actionId) match {
            case Some(actionToExecute) => {
              var newGameBoard: GameBoard = gameBoard.copy()
              var events: List[Event] = List[Event]()
              // Update view direction first to ensure correct view direction on action execution
              val newActivePlayer = activePlayer.copy(viewDirection = direction)
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
                  events = shootResult._2 ::: events
                case WAIT => // Do nothing
              }
              newGameBoard = updateActionPoints(newGameBoard, activePlayerNumber, actionToExecute)

              val nextTurn = updateTurn(Option(actionToExecute), newActivePlayer.actionPoints, newGameBoard)
              // Reset player actions points when turn changed
              if (nextTurn._2 != turnNumber) {
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
  }

  private def resetPlayerActionPoints(gameBoard: GameBoard, players: List[PlayerObject]): GameBoard = {
    if (!players.isEmpty) {
      val player = players.head
      val newGameBoard = gameBoard.placeGameObject(player.copy(actionPoints = player.maxActionPoints))
      resetPlayerActionPoints(newGameBoard, players.tail)
    } else {
      gameBoard
    }
  }

  private def updateActionPoints(gameBoard: GameBoard, playerNumber: Int, action: Action): GameBoard = {
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
          case Some(collisionObject) => playerOrBlockHit(collisionObject, player.position, gameBoard, shootAction)
          case None => (gameBoard, nothingHit(gameBoard, player.position, direction, shootAction))
        }
      }
      case None => (gameBoard, List())
    }
  }

  private def playerOrBlockHit(collisionObject: GameObject, playerPosition: Position, gameBoard: GameBoard, shootAction: Action): (GameBoard, List[Event]) = {
    collisionObject match {
      case playerObjectHit: PlayerObject =>
        val newGameBoard = damageAndRemoveDeadPlayer(playerObjectHit, gameBoard, shootAction)
        val events = List(CellChanged(List((playerObjectHit.position.rowIdx, playerObjectHit.position.columnIdx), (playerPosition.rowIdx, playerPosition.columnIdx))),
          AttackResult(collisionObject.position.rowIdx, collisionObject.position.columnIdx, hit = true, gameConfigProvider.attackImagePath, gameConfigProvider.attackSoundPath))
        (newGameBoard, events)
      case blockObject: BlockObject => {
        val events = List(CellChanged(List((blockObject.position.rowIdx, blockObject.position.columnIdx), (playerPosition.rowIdx, playerPosition.columnIdx))),
          AttackResult(collisionObject.position.rowIdx, collisionObject.position.columnIdx, hit = true, gameConfigProvider.attackImagePath, gameConfigProvider.attackSoundPath))
        (gameBoard, events)
      }
    }
  }

  private def nothingHit(gameBoard: GameBoard, playerPosition: Position, viewDirection: Direction, shootAction: Action): List[Event] = {
    gameBoard.calculatePositionForDirection(playerPosition, viewDirection, shootAction.range) match {
      case Some(targetPosition) => List(CellChanged(List((playerPosition.rowIdx, playerPosition.columnIdx))),
        AttackResult(targetPosition.rowIdx, targetPosition.columnIdx, hit = false, gameConfigProvider.attackImagePath, gameConfigProvider.attackSoundPath))
      case None => List()
    }
  }

  private def damageAndRemoveDeadPlayer(playerObject: PlayerObject, gameBoard: GameBoard, shootAction: Action): GameBoard = {
    val updatedPlayerObject = playerObject.copy(healthPoints = playerObject.healthPoints - shootAction.damage)
    if (updatedPlayerObject.healthPoints <= 0) {
      gameBoard.removeGameObject(updatedPlayerObject)
    } else {
      gameBoard.placeGameObject(updatedPlayerObject)
    }
  }

  private def updateTurn(lastAction: Option[Action], currentActionPoints: Int, currentGameBoard: GameBoard): (Int, Int) = {
    if (0 >= currentActionPoints) {
      val foundPlayer = nextPlayer(activePlayerNumber, currentGameBoard)
      // When next player number is lower than current one, every player has played his turn and we need to update turn counter
      if (foundPlayer.playerNumber > activePlayerNumber) {
        (foundPlayer.playerNumber, turnCounter)
      } else {
        val nextTurnNumber = turnCounter + 1
        (foundPlayer.playerNumber, nextTurnNumber)
      }
    } else {
      (playerNumber, turnNumber)
    }
  }

  private def nextPlayer(playerNumber: Int, gameBoard: GameBoard): PlayerObject = {
    val nextPlayerNumber = playerNumber + 1
    gameBoard.player(nextPlayerNumber) match {
      case Some(player) => player
      case None => {
        if (nextPlayerNumber >= gameBoard.players.size) {
          nextPlayer(0, gameBoard)
        } else {
          nextPlayer(nextPlayerNumber, gameBoard)
        }
      }
    }
  }

  override def lastExecutedActionId: Option[Int] = {
    lastExecutedAction match {
      case Some(action) => Option(action.id)
      case None => Option.empty[Int]
    }
  }

  override def canExecuteAction(actionId: Int, rowIndex: Int, columnIndex: Int): Future[Boolean] = {
    gameBoard.player(playerNumber) match {
      case Some(player) => canExecuteAction(actionId, gameBoard.calculateDirection(player.position, Position(rowIndex, columnIndex)))
      case None => Future(false)
    }
  }

  override def canExecuteAction(actionId: Int, direction: Direction): Future[Boolean] = {
    Future {
      winnerId match {
        case Some(_) => false
        case None => checkActionExecution(actionId, direction)
      }
    }
  }

  private def checkActionExecution(actionId: Int, direction: Direction): Boolean = {
    gameBoard.player(playerNumber) match {
      case Some(player) => player.actions.find(_.id == actionId) match {
        case Some(action) => {
          action.actionType match {
            case MOVE => {
              val newPositionOpt = gameBoard.calculatePositionForDirection(player.position, direction, action.range)
              newPositionOpt match {
                case Some(newPosition) => {
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
      case Some(gameObj) => {
        gameObj match {
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
      case Some(gameObj) => {
        gameObj match {
          case playerObj: PlayerObject => playerObj.playerNumber.toString
          case blockObj: BlockObject => blockObj.name
        }
      }
      case None => "X"
    }
  }

  override def cellContent(rowIndex: Int, columnIndex: Int): Option[GameObject] = {
    gameBoard.gameObjectAt(rowIndex, columnIndex)
  }

  override def cellsInRange(actionId: Option[Int]): Future[List[(Int, Int)]] = {
    Future {
      actionId match {
        case Some(action) => {
          gameBoard.player(playerNumber) match {
            case Some(player) => player.actions.find(_.id == action) match {
              case Some(actionFound) => gameBoard.reachableCells(player.position, actionFound)
              case None => List()
            }
            case None => List()
          }
        }
        case None => List()
      }
    }
  }

  override def rowCount: Int = {
    gameBoard.rows
  }

  override def columnCount: Int = {
    gameBoard.columns
  }

  override def actionDamage(actionId: Int): Int = {
    gameBoard.player(playerNumber) match {
      case Some(player) => player.actions.find(_.id == actionId) match {
        case Some(action) => action.damage
        case None => 0
      }
      case None => 0
    }
  }

  override def actionRange(actionId: Int): Int = {
    gameBoard.player(playerNumber) match {
      case Some(player) => player.actions.find(_.id == actionId) match {
        case Some(action) => action.range
        case None => 0
      }
      case None => 0
    }
  }

  override def wonImagePath: String = {
    winnerId match {
      case Some(playerNr) => player(playerNr) match {
        case Some(player) => player.wonImagePath
        case None => ""
      }
      case None => ""
    }
  }

  override def scenarioIds: Set[Int] = {
    gameConfigProvider.listScenarios.indices.toSet
  }

  override def scenarioName(scenarioId: Int): Option[String] = {
    if (scenarioId >= 0 && scenarioId < gameConfigProvider.listScenarios.size) {
      val scenarioName = gameConfigProvider.listScenarios(scenarioId)
      Option(scenarioName.substring(0, scenarioName.lastIndexOf('.')).replace('_', ' '))
    } else {
      Option.empty
    }
  }

  override def activePlayerActionPoints: Int = {
    gameBoard.player(playerNumber) match {
      case Some(player) => player.actionPoints
      case None => 0
    }
  }

  override def activePlayerHealthPoints: Int = {
    gameBoard.player(playerNumber) match {
      case Some(player) => player.healthPoints
      case None => 0
    }
  }

  override def save(name: String): Future[Int] = {
    gameConfigProvider match {
      case gameConfigProviderImpl: GameConfigProviderImpl => {
        // MongoDB
        val mongoDbDao = new MongoDbGameConfigDao
        mongoDbDao.insert(GameConfig(name, JsonConverter.gameConfigProviderWriter.writes(gameConfigProviderImpl).toString()))
        // Slick
        val dao = new SlickGameConfigDao
        dao.insert(GameConfig(name, JsonConverter.gameConfigProviderWriter.writes(gameConfigProviderImpl).toString()))
      }
    }
  }

  override def load(id: Int): Future[GameConfig] = {
    //    val dao = new SlickGameConfigDao
    //    dao.findById(id)
    val mongoDbDao = new MongoDbGameConfigDao
    mongoDbDao.findById(id)
  }

  override def saveGameIds: Future[Seq[Option[Int]]] = {
    val dao = new SlickGameConfigDao
    dao.findAll
  }
}
