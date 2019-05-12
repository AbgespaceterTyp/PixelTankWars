package de.htwg.se.msiwar.rest

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import de.htwg.ptw.common.util.GameConfigProviderImpl
import de.htwg.se.msiwar.aview.MainApp
import de.htwg.se.msiwar.util.JsonConverter
import play.api.libs.json.Json

import scala.util.{Failure, Success}

object Routes {
  implicit val um:Unmarshaller[HttpEntity, GameConfigProviderImpl] = {
    Unmarshaller.byteStringUnmarshaller.mapWithCharset { (data, charset) =>
      JsonConverter.gameConfigProviderReader.reads(Json.parse(data.toArray)).get
    }
  }

  val all =
    get {
      pathPrefix("commands" / Remaining) { line =>
        complete {
          "" + MainApp.tui.executeCommand(line)
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
            onComplete(MainApp.controller.canExecuteAction(actionId, rowIndex, columnIndex)) {
              case Success(canExecute) => complete("" + canExecute)
              case Failure(exception) => complete(exception)
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
        path("start") {
          entity(as[GameConfigProviderImpl]) {  (config)  =>
            complete {
              "" + MainApp.controller.startGame(Option(config))
            }
          }
        } ~
        pathPrefix("actions") {
          path("execute" / IntNumber / IntNumber / IntNumber) { (actionId, rowIndex, columnIndex) =>
            onComplete(MainApp.controller.executeAction(actionId, rowIndex, columnIndex)) {
              case Success(_) => complete(JsonConverter.gameBoardToJson().toString())
              case Failure(exception) => complete(exception)
            }
          }
        }
      }
}
