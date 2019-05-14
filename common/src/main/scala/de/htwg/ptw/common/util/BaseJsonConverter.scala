package de.htwg.ptw.common.util

import de.htwg.ptw.common.Direction
import de.htwg.ptw.common.Direction.Direction
import de.htwg.ptw.common.model._
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

  implicit def direction = new Writes[Direction] {
    override def writes(direction: Direction): JsValue = Json.obj(
      "value" -> direction.toString
    )
  }

  implicit def directionReader = new Reads[Direction] {
    override def reads(json: JsValue): JsResult[Direction] = {
      JsSuccess(
        Direction.withName(
          (json \ "value").as[String]
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

  implicit def positionReader = new Reads[Position] {
    override def reads(json: JsValue): JsResult[Position] = {
      JsSuccess(
        Position(
          (json \ "rowIdx").as[Int],
          (json \ "columnIdx").as[Int]
        )
      )
    }
  }

  implicit def gameObject = new Writes[GameObject] {
    def writes(gameObject: GameObject) = {
      writeGameObjectByType(gameObject)
    }
  }

  private def writeGameObjectByType(gameObject: GameObject): JsValue = {
    gameObject match {
      case player: PlayerObject => {
        Json.obj(
          "playerObject" -> Json.obj(
            "name" -> player.name,
            "imagePath" -> player.imagePath,
            "position" -> position.writes(player.position),
            "viewDirection" -> direction.writes(player.viewDirection),
            "playerNumber" -> player.playerNumber,
            "wonImagePath" -> player.wonImagePath,
            "actionPoints" -> player.actionPoints,
            "maxActionPoints" -> player.maxActionPoints,
            "healthPoints" -> player.healthPoints,
            "maxHealthPoints" -> player.maxHealthPoints,
            //actions: List[Action] // TODO create reader/writer for it
          ))
      }
      case block: BlockObject => {
        Json.obj(
          "blockObject" -> Json.obj(
            "name" -> block.name,
            "imagePath" -> block.imagePath,
            "position" -> position.writes(block.position)
          ))
      }
      case gameObj: GameObject => {
        Json.obj(
          "gameObject" -> Json.obj(
            "name" -> gameObj.name,
            "imagePath" -> gameObj.imagePath,
            "position" -> position.writes(gameObj.position)
          ))
      }
    }
  }

  implicit def blockObjectReader = new Reads[BlockObject] {
    override def reads(json: JsValue): JsResult[BlockObject] = {
      JsSuccess(
        BlockObject(
          (json \ "name").as[String],
          (json \ "imagePath").as[String],
          (json \ "position").as[Position])
      )
    }
  }

  implicit def playerObjectReader = new Reads[PlayerObject] {
    override def reads(json: JsValue): JsResult[PlayerObject] = {
      JsSuccess(
        PlayerObject(
          (json \ "name").as[String],
          (json \ "imagePath").as[String],
          (json \ "position").as[Position],
          (json \ "viewDirection").as[Direction],
          (json \ "playerNumber").as[Int],
          (json \ "wonImagePath").as[String],
          (json \ "actionPoints").as[Int],
          (json \ "maxActionPoints").as[Int],
          (json \ "healthPoints").as[Int],
          (json \ "maxHealthPoints").as[Int],
          List[Action]() // TODO create reader/writer for it
        ))
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
        (json \\ "playerObject").map(_.as[PlayerObject]).toList ++
          (json \\ "blockObject").map(_.as[BlockObject]).toList
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
