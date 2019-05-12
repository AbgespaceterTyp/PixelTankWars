package de.htwg.se.msiwar.aview

import de.htwg.ptw.common.model.{Action, GameBoard, GameObject}
import de.htwg.ptw.common.util.{GameConfigProvider, GameConfigProviderImpl}
import de.htwg.se.msiwar.aview.swing.SwingFrame
import de.htwg.se.msiwar.aview.tui.Tui
import de.htwg.se.msiwar.controller.ControllerImpl
import de.htwg.se.msiwar.model._

object MainApp {
  var gameConfigProvider: GameConfigProvider = GameConfigProviderImpl(List[GameObject](), "", "", "", "", "", "", 1, 1)
  val scenarioName: String = gameConfigProvider.listScenarios.head
  gameConfigProvider = gameConfigProvider.loadFromFile(scenarioName)

  val createdModel = GameModelImpl(gameConfigProvider, GameBoard(gameConfigProvider.rowCount, gameConfigProvider.colCount,
    gameConfigProvider.gameObjects), Option.empty[Action], 1, 1)

  val controller = ControllerImpl(createdModel)
  val swingFrame = new SwingFrame(controller)
  swingFrame.visible = true

  val tui = new Tui(controller)

  def main(args: Array[String]): Unit = {
    while (tui.executeCommand(readLine())) {}
  }
}
