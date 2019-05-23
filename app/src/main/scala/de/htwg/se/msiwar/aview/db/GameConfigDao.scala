package de.htwg.se.msiwar.aview.db

import de.htwg.se.msiwar.aview.db.model.{GameConfig, GameConfigTable, GameObjectConfigTable}
import slick.jdbc.H2Profile.api._

import scala.concurrent.Future

class GameConfigDao(implicit session: Session) {
  private lazy val db = Database.forConfig("h2mem1")
  private lazy val gameConfigs = TableQuery[GameConfigTable]
  private lazy val gameObjs = TableQuery[GameObjectConfigTable]

  private def findByIdQuery(id: Int) : Query[GameConfigTable, GameConfig, Seq] = {
    gameConfigs.filter(f => f.id == id)
  }

  def findById(id: Int) : Future[GameConfig]  = {
    try db.run(findByIdQuery(id).result.head)
    finally db.close
  }

  def insert(gameConfig: GameConfig): Future[Int] = {
    try db.run(gameConfigs += gameConfig)
    finally db.close
  }

  def update(id: Int, gameConfig: GameConfig): Future[Int] = {
    try db.run(findByIdQuery(id).update(gameConfig))
    finally db.close
  }

  def delete(id: Int): Future[Int] = {
    try db.run(findByIdQuery(id).delete)
    finally db.close
  }
}
