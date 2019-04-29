package de.htwg.se.msiwar

import de.htwg.se.msiwar.controller.Controller
import de.htwg.se.msiwar.model._

import scala.concurrent.Promise
import scala.swing.Publisher
import scala.util.Try

case class TestEventHandler(controller: Controller,
                            gameStartedPromise: Option[Promise[Boolean]],
                            couldNotGenerateGamePromise: Option[Promise[Boolean]],
                            turnStartedPromise: Option[Promise[Int]]) extends Publisher {

  this.listenTo(controller)

  reactions += {
    case _: CellChanged =>
    case _: PlayerStatsChanged =>
    case _: AttackResult =>
    case e: TurnStarted => turnStartedPromise.map(p => p.complete(Try(e.playerNumber)))
    case _: PlayerWon =>
    case _: GameStarted => gameStartedPromise.map(p => p.complete(Try(true)))
    case _: CouldNotGenerateGame => couldNotGenerateGamePromise.map(p => p.complete(Try(true)))
  }
}
