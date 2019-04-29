package de.htwg.se.msiwar.rest


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import de.htwg.se.msiwar.aview.MainApp
import de.htwg.se.msiwar.util.JsonConverter

import scala.io.StdIn

object WebServer {

  def main(args: Array[String]) {

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val route =
      get {
        pathPrefix("commands" / LongNumber) { line =>
          complete {
            "" + MainApp.tui.executeCommand(line.toString)
          }
        } ~
        pathPrefix("actions") {
          path("check"/ IntNumber / IntNumber / IntNumber) { (actionId, rowIndex, columnIndex) =>
            complete {
              "" + MainApp.controller.canExecuteAction(actionId, rowIndex, columnIndex)
            }
          } ~
          path("cost"/ IntNumber) { (actionId) =>
            complete {
              "" + MainApp.controller.actionPointCost(actionId)
            }
          } ~
          path("desc"/ IntNumber) { (actionId) =>
            complete {
              "" + MainApp.controller.actionDescription(actionId)
            }
          } ~
          path("icon"/ IntNumber) { (actionId) =>
            complete {
              "" + MainApp.controller.actionIconPath(actionId)
            }
          } ~
          path("damage"/ IntNumber) { (actionId) =>
            complete {
              "" + MainApp.controller.actionDamage(actionId)
            }
          } ~
          path("range"/ IntNumber) { (actionId) =>
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

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}