package de.htwg.se.msiwar.aview.swing

import de.htwg.se.msiwar.controller.Controller
import de.htwg.se.msiwar.model.{GameStarted, PlayerWon}
import de.htwg.se.msiwar.util.ImageUtils
import javax.swing.{JOptionPane, WindowConstants}

import scala.swing._

class SwingFrame(controller: Controller) extends Frame {
  private val contentPanel = new SwingPanel(controller)
  private val imageOpt = ImageUtils.loadImageIcon(controller.appIconImagePath)

  title = "Pixel Tank War"
  resizable = false
  contents = contentPanel

  if (imageOpt.isDefined) {
    iconImage = imageOpt.get.getImage
  }

  packAndCenter()
  peer.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)

  listenTo(controller)
  reactions += {
    case _: GameStarted =>
      contentPanel.rebuild()
      packAndCenter()
    case e: PlayerWon =>
      contentPanel.showPlayerWon(e)
      packAndCenter()
    case e: Error => JOptionPane.showMessageDialog(this.contentPanel.self, e.getMessage, "Fehlermeldung", JOptionPane.PLAIN_MESSAGE)
  }

  private def packAndCenter(): Unit = {
    pack()
    contentPanel.resize(peer.getWidth, peer.getHeight)
    // Center on monitor
    peer.setLocationRelativeTo(null)
  }
}