package de.htwg.se.msiwar.util

import de.htwg.ptw.common.model.{GameObject, Position}
import de.htwg.ptw.common.util.GameConfigProviderImpl
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

  implicit def positionReader = new Reads[Position] {
    override def reads(json: JsValue): JsResult[Position] = ???
  }

  implicit def position = new Writes[Position] {
    override def writes(position: Position): JsValue = Json.obj(
      "rowIdx" -> position.rowIdx,
      "columnIdx" -> position.columnIdx
    )
  }

  implicit def gameObject = new Writes[GameObject] {
    def writes(playerObject: GameObject) = Json.obj(
      "name" -> playerObject.name,
      "imagePath" -> playerObject.imagePath,
      "position" -> position.writes(playerObject.position)
    )
  }

  implicit def gameObjectReader = new Reads[GameObject] {
    override def reads(json: JsValue): JsResult[GameObject] = {
      JsSuccess(
        new GameObject(
          (json \ "name").as[String],
          (json \ "imagePath").as[String],
          (json \ "position").as[Position])
      )
    }
  }

  implicit def gameObjects = new Writes[List[GameObject]] {
    def writes(o: List[GameObject]): JsValue = {
      JsArray(o.map(Json.toJson(_)))
    }
  }

  implicit def gameObjectsReader = new Reads[List[GameObject]] {
    override def reads(json: JsValue): JsResult[List[GameObject]] = {
      JsSuccess(
        // TODO read list
        List[GameObject]()
      )
    }
  }

  implicit def gameConfigProvider = new Writes[GameConfigProviderImpl] {
    def writes(config: GameConfigProviderImpl) = Json.obj(
      "gameObjects" -> gameObjects.writes(config.gameObjects),
      "attackSoundPath" -> config.attackSoundPath,
      "openingBackgroundImagePath" -> config.openingBackgroundImagePath,
      "levelBackgroundImagePath" -> config.levelBackgroundImagePath,
      "actionbarBackgroundImagePath" -> config.actionbarBackgroundImagePath,
      "attackImagePath" -> config.attackImagePath,
      "appIconImagePath" -> config.appIconImagePath,
      "rowCount" -> config.rowCount,
      "colCount" -> config.colCount
    )
  }

  implicit def gameConfigProviderReader = new Reads[GameConfigProviderImpl] {
    override def reads(json: JsValue): JsResult[GameConfigProviderImpl] = {
      JsSuccess(
        GameConfigProviderImpl(
          (json \ "gameObjects").as[List[GameObject]],
          (json \ "attackSoundPath").as[String],
          (json \ "openingBackgroundImagePath").as[String],
          (json \ "levelBackgroundImagePath").as[String],
          (json \ "actionbarBackgroundImagePath").as[String],
          (json \ "attackImagePath").as[String],
          (json \ "appIconImagePath").as[String],
          (json \ "rowCount").as[Int],
          (json \ "colCount").as[Int]
        )
      )
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