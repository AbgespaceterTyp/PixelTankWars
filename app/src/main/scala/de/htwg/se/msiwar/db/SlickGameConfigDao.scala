package de.htwg.se.msiwar.db

import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class SlickGameConfigDao extends BaseDao {
  private lazy val db = Database.forConfig("h2mem1")
  private lazy val gameConfigs = TableQuery[GameConfigTable]

  private def findByIdQuery(id: Int): Query[GameConfigTable, GameConfig, Seq] = {
    gameConfigs.filter(f => f.id === id)
  }

  override def findAll: Future[Seq[Option[Int]]] = {
    db.run(gameConfigs.result.map(_.map(f => f.id)))
  }

  override def findById(id: Int): Future[GameConfig] = {
    db.run(findByIdQuery(id).result.head)
  }

  override def insert(gameConfig: GameConfig): Future[Int] = {
    Await.result(db.run(gameConfigs.schema.createIfNotExists), Duration.Inf)
    db.run(gameConfigs += gameConfig)
  }

  override def update(id: Int, gameConfig: GameConfig): Future[Int] = {
    db.run(findByIdQuery(id).update(gameConfig))
  }

  override def delete(id: Int): Future[Int] = {
    db.run(findByIdQuery(id).delete)
  }
}
