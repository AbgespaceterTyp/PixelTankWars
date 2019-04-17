package de.htwg.se.msiwar.model

import akka.actor.Actor

import scala.util.{Failure, Success, Try}

sealed trait GenerationMessage

case object Generate extends GenerationMessage

case class Work(rowCount: Int, columnCount: Int) extends GenerationMessage

case class Result(gameObjectsOpt: Option[List[GameObject]]) extends GenerationMessage

class GameGenerationWorker extends Actor {

  def generate(rowCount: Int, columnCount: Int): Option[List[GameObject]] = {
    Try(GameGenerator(rowCount, columnCount)) match {
      case Success(gameGenerator) => gameGenerator.generate()
      case Failure(_) => Option.empty
    }
  }

  def receive: PartialFunction[Any, Unit] = {
    case Work(rowCount: Int, columnCount: Int) =>
      sender ! Result(generate(rowCount, columnCount))
  }
}
