package de.htwg.ptw.common.util

import de.htwg.ptw.common.model.{GameObject, Position}
import play.api.libs.json._

class BaseJsonConverter {

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
    override def reads(json: JsValue): JsResult[Position] = {
      JsSuccess(
        new Position(
          (json \ "rowIdx").as[Int],
          (json \ "columnIdx").as[Int]
        )
      )
    }
  }

  implicit def position = new Writes[Position] {
    override def writes(position: Position): JsValue = Json.obj(
      "rowIdx" -> position.rowIdx,
      "columnIdx" -> position.columnIdx
    )
  }

  implicit def gameObject = new Writes[GameObject] {
    def writes(playerObject: GameObject) = Json.obj(
      "gameObject" -> Json.obj(
        "name" -> playerObject.name,
        "imagePath" -> playerObject.imagePath,
        "position" -> position.writes(playerObject.position)
      )
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
          (json \\ "gameObject").map(_.as[GameObject]).toList,
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
          //(json \ "gameObjects" \\ "gameObject").map(_.as[GameObject]).toList,
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
}
