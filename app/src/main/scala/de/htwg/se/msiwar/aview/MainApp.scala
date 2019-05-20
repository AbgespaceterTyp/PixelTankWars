package de.htwg.se.msiwar.aview

import java.awt.GraphicsEnvironment

import de.htwg.ptw.common.model.{Action, GameBoard, GameObject}
import de.htwg.ptw.common.util.{GameConfigProvider, GameConfigProviderImpl}
import de.htwg.se.msiwar.aview.swing.SwingFrame
import de.htwg.se.msiwar.aview.tui.Tui
import de.htwg.se.msiwar.controller.ControllerImpl
import de.htwg.se.msiwar.model._
import de.htwg.se.msiwar.rest.WebServer

object MainApp {
  var gameConfigProvider: GameConfigProvider = GameConfigProviderImpl(List[GameObject](), "sounds/explosion.wav", "images/background_opening.png",
    "", "images/background_actionbar.png", "images/hit.png", "images/app_icon.png", 1, 1)

  val createdModel = GameModelImpl(gameConfigProvider, GameBoard(gameConfigProvider.rowCount, gameConfigProvider.colCount,
    gameConfigProvider.gameObjects), Option.empty[Action], 1, 1)

  val controller = ControllerImpl(createdModel)

  // Start swing app only when not in headless environment
  if (!GraphicsEnvironment.isHeadless()) {
    val swingFrame = new SwingFrame(controller)
    swingFrame.visible = true
  }

  val tui = new Tui(controller)

  // Start web server in own thread for messaging handling
  val thread = new Thread {
    override def run {
      val webServer = new WebServer
      webServer.start
    }
  }
  thread.start()

  def main(args: Array[String]): Unit = {
    while (tui.executeCommand(readLine())) {}
  }
}
