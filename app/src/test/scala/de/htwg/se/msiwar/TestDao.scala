package de.htwg.se.msiwar

import de.htwg.se.msiwar.db.{BaseDao, GameConfig}

import scala.concurrent.Future

class TestDao extends BaseDao{
  override def findAll: Future[Seq[Option[Int]]] = ???

  override def findById(id: Int): Future[GameConfig] = ???

  override def insert(gameConfig: GameConfig): Future[Int] = ???

  override def update(id: Int, gameConfig: GameConfig): Future[Int] = ???

  override def delete(id: Int): Future[Int] = ???
}
