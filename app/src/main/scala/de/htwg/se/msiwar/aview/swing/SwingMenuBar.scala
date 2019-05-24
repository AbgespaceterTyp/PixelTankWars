package de.htwg.se.msiwar.aview.swing

import de.htwg.se.msiwar.controller.Controller

import scala.swing.event.Key
import scala.swing.{Action, Dialog, Menu, MenuBar, MenuItem, Separator}

class SwingMenuBar(controller: Controller) extends MenuBar {

  contents += new Menu("Game") {
    mnemonic = Key.G

    contents += new MenuItem(Action("Random Level") {
      controller.startRandomGame()
    })

    contents += new Separator()

    controller.scenarioIds.foreach(s => {
      val scenarioNameOpt = controller.scenarioName(s)
      if (scenarioNameOpt.isDefined) {
        contents += new MenuItem(Action(scenarioNameOpt.get) {
          controller.startGame(s)
        })
      }
    }
    )
    contents += new Separator()
    contents += new MenuItem(Action("Save...") {
      val saveGameNameDialog = Dialog.showInput(null, "Bitte eine Bezeichnung für den Spielstand eingeben", initial="Neues Spiel")
      controller.save(saveGameNameDialog.get)
    })

    contents += new MenuItem(Action("Load...") {
      controller.load(1)
    })
  }

  contents += new Menu("Help") {
    mnemonic = Key.H

    contents += new MenuItem(Action("Controls...") {
      val controlsDialog = new SwingControlsDialog
      controlsDialog.visible = true
    })

    contents += new MenuItem(Action("About...") {
      val aboutDialog = new SwingAboutDialog
      aboutDialog.visible = true
    })
  }
}
