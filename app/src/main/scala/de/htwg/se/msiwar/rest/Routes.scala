package de.htwg.se.msiwar.rest

import akka.http.scaladsl.server.Directives._
import de.htwg.se.msiwar.aview.MainApp
import de.htwg.se.msiwar.util.JsonConverter

object Routes {

  val all =
  get {
    pathPrefix("commands" / LongNumber) { line =>
      complete {
        "" + MainApp.tui.executeCommand(line.toString)
      }
    } ~
    pathPrefix("cells") {
      path("") {
        complete {
          JsonConverter.gameBoardToJson().toString()
        }
      } ~
      path(IntNumber / IntNumber) { (rowIndex, columnIndex) =>
        complete {
          MainApp.controller.cellContent(rowIndex, columnIndex) match {
            case Some(value) => JsonConverter.gameObject.writes(value).toString()
            case None => ""
          }
        }
      } ~
      path("text" / IntNumber / IntNumber) { (rowIndex, columnIndex) =>
        complete {
          MainApp.controller.cellContentToText(rowIndex, columnIndex)
        }
      } ~
      path("image" / IntNumber / IntNumber) { (rowIndex, columnIndex) =>
        complete {
          MainApp.controller.cellContentImagePath(rowIndex, columnIndex) match {
            case Some(value) => value
            case None => ""
          }
        }
      }
    } ~
    pathPrefix("actions") {
      path(IntNumber / "check" / IntNumber / IntNumber) { (actionId, rowIndex, columnIndex) =>
        complete {
          "" + MainApp.controller.canExecuteAction(actionId, rowIndex, columnIndex)
        }
      } ~
      path(IntNumber / "cost") { (actionId) =>
        complete {
          "" + MainApp.controller.actionPointCost(actionId)
        }
      } ~
      path(IntNumber / "desc") { (actionId) =>
        complete {
          "" + MainApp.controller.actionDescription(actionId)
        }
      } ~
      path(IntNumber / "icon") { (actionId) =>
        complete {
          "" + MainApp.controller.actionIconPath(actionId)
        }
      } ~
      path(IntNumber / "damage") { (actionId) =>
        complete {
          "" + MainApp.controller.actionDamage(actionId)
        }
      } ~
      path(IntNumber / "range") { (actionId) =>
        complete {
          "" + MainApp.controller.actionRange(actionId)
        }
      }
    }
  } ~
  put {
    pathPrefix("actions") {
      path("execute" / IntNumber / IntNumber / IntNumber) { (actionId, rowIndex, columnIndex) =>
        complete {
          MainApp.controller.executeAction(actionId, rowIndex, columnIndex)
          JsonConverter.gameBoardToJson().toString()
        }
      }
    }
  }
}
