package de.htwg.se.msiwar.rest


import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.io.StdIn

class WebServer {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = system.dispatcher

  def start {
    val bindingFuture = Http().bindAndHandle(Routes.all, "localhost", 8080)
    println(s"Server online at http://app:8080/")
    while (true) {
      Thread.sleep(100)
    }
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}