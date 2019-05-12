package de.htwg.ptw.generator.util

import de.htwg.ptw.common.model.{GameObject, Position}
import de.htwg.ptw.common.util.GameConfigProviderImpl
import play.api.libs.json._

object JsonConverter {

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

  implicit def gameObjects = new Writes[List[GameObject]] {
    def writes(o: List[GameObject]): JsValue = {
      JsArray(o.map(Json.toJson(_)))
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
      "appIconImagePath" -> config.appIconImagePath
    )
  }
}