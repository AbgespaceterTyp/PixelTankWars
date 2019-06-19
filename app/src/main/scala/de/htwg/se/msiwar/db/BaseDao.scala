package de.htwg.se.msiwar.db

import scala.concurrent.Future

trait BaseDao {

  def findAll: Future[Seq[Option[Int]]]

  def findById(id: Int): Future[GameConfig]

  def insert(gameConfig: GameConfig): Future[Int]

  def update(id: Int, gameConfig: GameConfig): Future[Int]

  def delete(id: Int): Future[Int]
}
