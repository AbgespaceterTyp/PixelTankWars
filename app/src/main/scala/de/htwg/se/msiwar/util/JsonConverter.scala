package de.htwg.se.msiwar.util

import de.htwg.ptw.common.model.GameObject
import de.htwg.se.msiwar.aview.MainApp.controller
import de.htwg.se.msiwar.model._
import play.api.libs.json._

import scala.collection.mutable

object JsonConverter {

  implicit def tuple = new Writes[(Int, Int)] {
    override def writes(o: (Int, Int)) = Json.obj(
      "rowIdx" -> o._1,
      "columnIdx" -> o._2,
    )
  }


  implicit def tuples = new Writes[List[(Int, Int)]] {

    override def writes(o: List[(Int, Int)]): JsValue = {
      JsArray(o.map(Json.toJson(_)))
    }
  }


  implicit def gameObject = new Writes[GameObject] {

    def writes(playerObject: GameObject) = Json.obj(
      "rowIdx" -> playerObject.position.rowIdx,
      "columnIdx" -> playerObject.position.columnIdx,
      "imagePath" -> playerObject.imagePath,
    )
  }


  implicit def gameObjects = new Writes[List[GameObject]] {

    def writes(o: List[GameObject]): JsValue = {
      JsArray(o.map(Json.toJson(_)))
    }
  }


  implicit def playerWon = new Writes[PlayerWon] {

    override def writes(playerWonEvent: PlayerWon): JsValue = {
      Json.obj(
        "eventType" -> PlayerWon.getClass.getSimpleName,
        "playerNumber" -> playerWonEvent.playerNumber,
        "wonImagePath" -> playerWonEvent.wonImagePath,
      )
    }
  }


  implicit def turnStarted = new Writes[TurnStarted] {

    override def writes(turnStartedEvent: TurnStarted): JsValue = {
      Json.obj(
        "eventType" -> TurnStarted.getClass.getSimpleName,
        "playerNumber" -> turnStartedEvent.playerNumber,
        "playerName" -> controller.activePlayerName,
        "hp" -> controller.activePlayerHealthPoints,
        "ap" -> controller.activePlayerActionPoints,
        "actions" -> actionsForPlayerToJson(turnStartedEvent.playerNumber)
      )
    }
  }


  implicit def attackResult = new Writes[AttackResult] {

    override def writes(attackResultEvent: AttackResult): JsValue = {
      Json.obj(
        "eventType" -> AttackResult.getClass.getSimpleName,
        "rowIdx" -> attackResultEvent.rowIndex,
        "columnIdx" -> attackResultEvent.columnIndex,
        "hit" -> attackResultEvent.hit,
        "imagePath" -> attackResultEvent.attackImagePath,
        "soundPath" -> attackResultEvent.attackSoundPath,
      )
    }
  }

  def playerWonToJson(playerWonEvent: PlayerWon): JsValue = {

    playerWon.writes(playerWonEvent)

  }

  def turnStartedToJson(turnStartedEvent: TurnStarted): JsValue = {

    turnStarted.writes(turnStartedEvent)

  }

  def attackResultToJson(attackResultEvent: AttackResult): JsValue = {

    attackResult.writes(attackResultEvent)

  }

  def gameBoardToJson(): JsValue = {

    val list = mutable.MutableList[GameObject]()

    for (row <- 0 until controller.rowCount) {
      for (col <- 0 until controller.columnCount) {
        val gameObjectOpt = controller.cellContent(row, col)
        if (gameObjectOpt.isDefined) {
          list += gameObjectOpt.get
        }
      }
    }
    JsonConverter.gameObjects.writes(list.toList)
  }

  def playerStatusToJson(): JsValue = {
    Json.obj(
      "playerName" -> controller.activePlayerName,
      "hp" -> controller.activePlayerHealthPoints,
      "ap" -> controller.activePlayerActionPoints,
    )
  }

  def actionsForPlayerToJson(playerId: Int): JsValue = {
    var actions = new JsArray()

    controller.actionIds(playerId).foreach(actionId => actions = actions :+ Json.obj(
      "id" -> actionId,
      "damage" -> controller.actionDamage(actionId),
      "description" -> controller.actionDescription(actionId),
      "iconPath" -> controller.actionIconPath(actionId),
      "range" -> controller.actionRange(actionId),
      "cost" -> controller.actionPointCost(actionId),
    ))
    actions
  }
}