package de.htwg.se.msiwar.db

import slick.jdbc.H2Profile.api._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object GameConfigDao {
  private lazy val db = Database.forConfig("h2mem1")
  private lazy val gameConfigs = TableQuery[GameConfigTable]

  private def findByIdQuery(id: Int): Query[GameConfigTable, GameConfig, Seq] = {
    gameConfigs.filter(f => f.id === id)
  }

  def findAll: Future[Seq[Option[Int]]] = {
    db.run(gameConfigs.result.map(_.map(f => f.id)))
  }

  def findById(id: Int): Future[GameConfig] = {
    db.run(findByIdQuery(id).result.head)
  }

  def insert(gameConfig: GameConfig): Future[Int] = {
    Await.result(db.run(gameConfigs.schema.createIfNotExists), Duration.Inf)
    db.run(gameConfigs += gameConfig)
  }

  def update(id: Int, gameConfig: GameConfig): Future[Int] = {
    db.run(findByIdQuery(id).update(gameConfig))
  }

  def delete(id: Int): Future[Int] = {
    db.run(findByIdQuery(id).delete)
  }
}
