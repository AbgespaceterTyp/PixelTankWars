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
        pathPrefix("command") {
          parameters('line.as[String]) { line =>
            complete {
              "" + MainApp.tui.executeCommand(line)
            }
          }
        } ~
        pathPrefix("action") {
          path("check") {
            parameters('actionId.as[Int], 'rowIndex.as[Int], 'columnIndex.as[Int]) { (actionId, rowIndex, columnIndex) =>
              complete {
                "" + MainApp.controller.canExecuteAction(actionId, rowIndex, columnIndex)
              }
            }
          }
        }
      } ~
      put {
        pathPrefix("action") {
          path("execute") {
            parameters('actionId.as[Int], 'rowIndex.as[Int], 'columnIndex.as[Int]) { (actionId, rowIndex, columnIndex) =>
              complete {
                MainApp.controller.executeAction(actionId, rowIndex, columnIndex)
                JsonConverter.gameBoardToJson().toString()
              }
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