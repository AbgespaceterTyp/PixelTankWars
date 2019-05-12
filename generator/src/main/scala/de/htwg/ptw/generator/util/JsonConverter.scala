package de.htwg.ptw.generator.util

import de.htwg.ptw.common.model.GameObject
import de.htwg.ptw.common.util.GameConfigProviderImpl
import play.api.libs.json._

object JsonConverter {

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
}