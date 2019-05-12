package de.htwg.ptw.generator

import akka.actor.Actor
import de.htwg.ptw.common.model.GameObject

import scala.util.{Failure, Success, Try}

sealed trait GenerationMessage

case class Generate(rowCount: Int, columnCount: Int) extends GenerationMessage

case class Work(rowCount: Int, columnCount: Int) extends GenerationMessage

case class Result(gameObjectsOpt: Option[List[GameObject]], rowCount: Int, colCount: Int) extends GenerationMessage

class GameGenerationWorker extends Actor {

  def generate(rowCount: Int, columnCount: Int): Option[List[GameObject]] = {
    Try(GameGenerator(rowCount, columnCount)) match {
      case Success(gameGenerator) => gameGenerator.generate()
      case Failure(_) => Option.empty
    }
  }

  def receive: PartialFunction[Any, Unit] = {
    case Work(rowCount: Int, columnCount: Int) =>
      sender ! Result(generate(rowCount, columnCount), rowCount, columnCount)
  }
}
