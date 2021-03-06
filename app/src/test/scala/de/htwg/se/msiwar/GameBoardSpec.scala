package de.htwg.se.msiwar

import de.htwg.ptw.common.model._
import de.htwg.ptw.common.{ActionType, Direction}
import org.scalatest.{FlatSpec, Matchers}


class GameBoardSpec extends FlatSpec with Matchers {
  
  it should "return LEFT_UP for the given positions" in {
    val direction = GameBoard(10, 10, List()).calculateDirection(Position(1, 1), Position(0, 0))

    direction should be(Direction.LEFT_UP)
  }

  it should "return LEFT_DOWN for the given positions" in {
    val direction = GameBoard(10, 10, List()).calculateDirection(Position(1, 1), Position(2, 0))

    direction should be(Direction.LEFT_DOWN)
  }

  it should "return LEFT for the given positions" in {
    val direction = GameBoard(10, 10, List()).calculateDirection(Position(1, 1), Position(1, 0))

    direction should be(Direction.LEFT)
  }

  it should "return RIGHT for the given positions" in {
    val direction = GameBoard(10, 10, List()).calculateDirection(Position(1, 1), Position(1, 2))

    direction should be(Direction.RIGHT)
  }

  it should "return RIGHT_UP for the given positions" in {
    val direction = GameBoard(10, 10, List()).calculateDirection(Position(1, 1), Position(0, 2))

    direction should be(Direction.RIGHT_UP)
  }

  it should "return RIGHT_DOWN for the given positions" in {
    val direction = GameBoard(10, 10, List()).calculateDirection(Position(1, 1), Position(2, 2))

    direction should be(Direction.RIGHT_DOWN)
  }

  it should "return DOWN for the given positions" in {
    val direction = GameBoard(10, 10, List()).calculateDirection(Position(1, 1), Position(2, 1))

    direction should be(Direction.DOWN)
  }

  it should "return UP for the given positions" in {
    val direction = GameBoard(10, 10, List()).calculateDirection(Position(1, 1), Position(0, 1))

    direction should be(Direction.UP)
  }

  it should "return a collision object when searching for it in the same column" in {
    val wood = BlockObject("B", "images/block_wood.png", Position(0, 5))

    val gameBoard = GameBoard(10, 10, List(wood))
    val collisionObject = gameBoard.collisionObject(Position(0, 0), Position(0, 7), false)
    collisionObject.get should be(wood)
  }

  it should "return a collision object when searching for it in the same row" in {
    val wood = BlockObject("B", "images/block_wood.png", Position(5, 0))

    val gameBoard = GameBoard(10, 10, List(wood))
    val collisionObject = gameBoard.collisionObject(Position(0, 0), Position(7, 0), false)
    collisionObject.get should be(wood)
  }

  it should "return no collision object when last position is ignored" in {
    val wood = BlockObject("B", "images/block_wood.png", Position(7, 0))

    val gameBoard = GameBoard(10, 10, List(wood))
    val collisionObject = gameBoard.collisionObject(Position(0, 0), Position(7, 0), true)
    collisionObject.isEmpty should be(true)
  }

  it should "return the moved GameObject from the new position" in {
    val oldPosition = Position(7, 6)
    val newPosition = Position(7, 7)

    val player = PlayerObject("Player1", "images/medium_tank_blue.png", oldPosition.copy(), Direction.LEFT,
      playerNumber = 1, "images/background_won_blue.png", actionPoints = 3, maxActionPoints = 3, healthPoints = 3, maxHealthPoints = 3, List())

    var gameBoard = GameBoard(10, 10, List(player))

    gameBoard = gameBoard.moveGameObject(player, newPosition)
    gameBoard.gameObjectAt(newPosition).get should be(gameBoard.player(1).get)
    gameBoard.gameObjectAt(oldPosition).isEmpty should be(true)
    gameBoard.player(1).get.position.columnIdx should be(7)
    gameBoard.player(1).get.position.rowIdx should be(7)
  }

  it should "return no GameObject when it is removed" in {
    val position = Position(7, 6)
    val wood = BlockObject("B", "images/block_wood.png", position)

    val gameBoard = GameBoard(10, 10, List(wood))
    gameBoard.removeGameObject(wood)

    gameBoard.gameObjectAt(position).isEmpty should be(true)
  }

  it should "return the cell 0,0 because it is reachable for shooting" in {
    val shootAction = Action(id = 1, "Move", "", "",
      actionPoints = 3, range = 1, ActionType.SHOOT, damage = 2)

    val wood = BlockObject("B", "images/block_wood.png", Position(0, 0))

    val gameBoard = GameBoard(10, 10, List(wood))
    gameBoard.reachableCells(Position(1, 1), shootAction)
      .exists(p => p._1 == 0 && p._2 == 0) should be(true)
  }

  it should "not return the cell 0,0 because it is not reachable for moving" in {
    val shootAction = Action(id = 1, "Move", "", "",
      actionPoints = 1, range = 2, ActionType.MOVE, damage = 2)

    val wood = BlockObject("B", "images/block_wood.png", Position(0, 0))

    val gameBoard = GameBoard(10, 10, List(wood))
    !gameBoard.reachableCells(Position(2, 2), shootAction)
      .exists(p => p._1 == 0 && p._2 == 0) should be(true)
  }
}
