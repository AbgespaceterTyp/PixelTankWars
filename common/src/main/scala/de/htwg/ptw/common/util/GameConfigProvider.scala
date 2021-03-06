package de.htwg.ptw.common.util

import de.htwg.ptw.common.model.GameObject

trait GameConfigProvider {
  def attackSoundPath: String

  def openingBackgroundImagePath: String

  def levelBackgroundImagePath: String

  def actionbarBackgroundImagePath: String

  def attackImagePath: String

  def appIconImagePath: String

  def gameObjects: List[GameObject]

  def listScenarios: List[String]

  def loadFromFile(configFilePath: String): GameConfigProvider

  def rowCount: Int

  def colCount: Int
}
