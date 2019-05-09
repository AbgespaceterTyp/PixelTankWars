package de.htwg.ptw.common.model

import de.htwg.ptw.common.ActionType.ActionType
import de.htwg.ptw.common.Direction.Direction
import de.htwg.ptw.common.util.IterationFunction
import de.htwg.ptw.common.{ActionType, Direction}

import scala.Option.empty

case class GameBoard(rows: Int, columns: Int, private val gameObjects: List[GameObject]) {

  private val board = Array.ofDim[GameObject](rows, columns)
  gameObjects.foreach(o => board(o.position.rowIdx)(o.position.columnIdx) = o)

  def placeGameObject(gameObject: GameObject): GameBoard = {
    board(gameObject.position.rowIdx)(gameObject.position.columnIdx) = gameObject
    copy(gameObjects = board.flatten.collect({ case s: PlayerObject => s case o: BlockObject => o }).toList)
  }

  def gameObjectAt(position: Position): Option[GameObject] = {
    gameObjectAt(position.rowIdx, position.columnIdx)
  }

  def isInBound(position: Position): Boolean = {
    (position.rowIdx >= 0 && position.columnIdx >= 0) &&
      (position.rowIdx < rows && position.columnIdx < columns)
  }

  def gameObjectAt(rowIndex: Int, columnIndex: Int): Option[GameObject] = {
    val objectAt = board(rowIndex)(columnIndex)
    Option(objectAt)
  }

  def moveGameObject(gameObject: GameObject, newPosition: Position): GameBoard = {
    board(gameObject.position.rowIdx)(gameObject.position.columnIdx) = null
    gameObject match {
      case p: PlayerObject => board(newPosition.rowIdx)(newPosition.columnIdx) = p.copy(position = newPosition)
      case b: BlockObject => board(newPosition.rowIdx)(newPosition.columnIdx) = b.copy(position = newPosition)
    }
    copy(gameObjects = board.flatten.collect({ case s: PlayerObject => s case o: BlockObject => o }).toList)
  }

  def removeGameObject(gameObject: GameObject): GameBoard = {
    board(gameObject.position.rowIdx)(gameObject.position.columnIdx) = null
    copy(gameObjects = board.flatten.collect({ case s: PlayerObject => s case o: BlockObject => o }).toList)
  }

  def collisionObject(from: Position, to: Position, ignoreLastPosition: Boolean): Option[GameObject] = {
    var collisionObject: Option[GameObject] = empty
    if (isInBound(to) && isInBound(from)) {
      var modifyPositionFunction: (Int, Int) => (Int, Int) = IterationFunction.changeNothing
      var range = 0

      if (from.rowIdx != to.rowIdx) {
        range = math.abs(from.rowIdx - to.rowIdx)
      } else if (from.columnIdx != to.columnIdx) {
        range = math.abs(from.columnIdx - to.columnIdx)
      }
      modifyPositionFunction = modifyPositionFunctionForDirection(calculateDirection(from, to))

      if (ignoreLastPosition) {
        range -= 1
      }
      IterationFunction.performOnPositionNTimes((from.rowIdx, from.columnIdx), range, modifyPositionFunction, (rowIdx, columnIdx) => {
        val pos = Position(rowIdx, columnIdx)
        if (isInBound(pos) && collisionObject.isEmpty) {
          val gameObject = gameObjectAt(pos)
          if (gameObject.isDefined) {
            collisionObject = Option(gameObject.get)
          }
        }
      }
      )
    }
    collisionObject
  }

  def players: List[PlayerObject] = {
    board.flatten.collect({ case s: PlayerObject => s }).toList
  }

  def player(playerNumber: Int): Option[PlayerObject] = {
    players.find(_.playerNumber == playerNumber)
  }

  private def modifyPositionFunctionForDirection(direction: Direction): (Int, Int) => (Int, Int) = {
    direction match {
      case Direction.RIGHT => IterationFunction.incColumnIdx
      case Direction.RIGHT_UP => IterationFunction.decRowIdxIncColumnIdx
      case Direction.RIGHT_DOWN => IterationFunction.incRowIdxIncColumnIdx
      case Direction.LEFT => IterationFunction.decColumnIdx
      case Direction.LEFT_UP => IterationFunction.decRowIdxDecColumnIdx
      case Direction.LEFT_DOWN => IterationFunction.incRowIdxDecColumnIdx
      case Direction.UP => IterationFunction.decRowIdx
      case Direction.DOWN => IterationFunction.incRowIdx
    }
  }

  private def addPosToListIfValid(position: Position, basePosition: Position, cellList: List[(Int, Int)], actionType: ActionType): List[(Int, Int)] = {
    var addToList = false
    if (isInBound(position)) {
      val gameObjectOpt = gameObjectAt(position)
      val rowOffSet = math.abs(position.rowIdx - basePosition.rowIdx)
      val columnOffSet = math.abs(position.columnIdx - basePosition.columnIdx)
      var collisionObjectInBetween = false
      if (rowOffSet > 1 || columnOffSet > 1) {
        collisionObjectInBetween = collisionObject(basePosition, position, ignoreLastPosition = true).isDefined
      }

      actionType match {
        case _: ActionType.SHOOT.type =>
          if (position != basePosition && !collisionObjectInBetween) {
            addToList = true
          }
        case _: ActionType.MOVE.type =>
          if (position != basePosition && gameObjectOpt.isEmpty) {
            addToList = true
          }
      }
    }
    if (addToList) {
      (position.rowIdx, position.columnIdx) :: cellList
    } else {
      cellList
    }
  }

  def reachableCells(position: Position, action: Action): List[(Int, Int)] = {
    var reachableCellsList = List[(Int, Int)]()
    val range = action.range

    action.actionType match {
      case _: ActionType.WAIT.type => reachableCellsList = (position.rowIdx, position.columnIdx) :: reachableCellsList
      case _ =>
        cellsInRange(position, range).foreach(positionInRange => {
          reachableCellsList = addPosToListIfValid(positionInRange, position, reachableCellsList, action.actionType)
        })
    }
    reachableCellsList
  }

  def cellsInRange(position: Position, range: Int): List[Position] = {
    var cellsInRangeList = List[Position]()
    val loopFunctions = IterationFunction.incRowIdx _ :: IterationFunction.incColumnIdx _ :: IterationFunction.decRowIdx _ :: IterationFunction.decColumnIdx _ :: IterationFunction.incRowIdxIncColumnIdx _ :: IterationFunction.decRowIdxDecColumnIdx _ :: IterationFunction.incRowIdxDecColumnIdx _ :: IterationFunction.decRowIdxIncColumnIdx _ :: Nil

    loopFunctions.foreach(f => {
      IterationFunction.performOnPositionNTimes((position.rowIdx, position.columnIdx), range, f, (rowIdx, columnIdx) => {
        cellsInRangeList = Position(rowIdx, columnIdx) :: cellsInRangeList
      })
    })
    cellsInRangeList
  }


  def calculateDirection(from: Position, to: Position): Direction = {
    if (from.columnIdx > to.columnIdx) {
      if (from.rowIdx < to.rowIdx) {
        Direction.LEFT_DOWN
      } else if (from.rowIdx > to.rowIdx) {
        Direction.LEFT_UP
      } else {
        Direction.LEFT
      }
    } else if (from.columnIdx < to.columnIdx) {
      if (from.rowIdx < to.rowIdx) {
        Direction.RIGHT_DOWN
      } else if (from.rowIdx > to.rowIdx) {
        Direction.RIGHT_UP
      } else {
        Direction.RIGHT
      }
    } else {
      if (from.rowIdx < to.rowIdx) {
        Direction.DOWN
      } else {
        Direction.UP
      }
    }
  }

  def calculatePositionForDirection(oldPosition: Position, direction: Direction, range: Int): Option[Position] = {
    var newPosition: Option[Position] = empty
    var modifyPositionFunction: (Int, Int) => (Int, Int) = IterationFunction.changeNothing

    modifyPositionFunction = modifyPositionFunctionForDirection(direction)
    IterationFunction.performOnPositionNTimes((oldPosition.rowIdx, oldPosition.columnIdx), range, modifyPositionFunction, (rowIdx, columnIdx) => {
      val pos = Position(rowIdx, columnIdx)
      if (isInBound(pos)) {
        // The last position which is in bound
        newPosition = Option(pos)
      }
    })
    newPosition
  }
}
