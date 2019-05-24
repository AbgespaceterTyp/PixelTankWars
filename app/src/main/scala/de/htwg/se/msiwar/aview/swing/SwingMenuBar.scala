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
      val saveGameNameDialog = Dialog.showInput(null, "Bitte eine Bezeichnung für den Spielstand eingeben:", initial="Neues Spiel")
      saveGameNameDialog match {
        case None => println("Keine Bezeichnung eingegeben, Spiel wird nicht gespeichert.")
        case Some(saveGameName) => controller.save(saveGameName)
      }
    })

    contents += new MenuItem(Action("Load...") {
      val saveGameNameDialog = Dialog.showInput(null, "Bitte die Spielstand Id eingeben:", initial = "1")
      saveGameNameDialog match {
        case None => println("Keine ID eingegeben, Spiel wird nicht geladen.")
        case Some(gameIdToLoad) => controller.load(gameIdToLoad.toInt)
      }
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
