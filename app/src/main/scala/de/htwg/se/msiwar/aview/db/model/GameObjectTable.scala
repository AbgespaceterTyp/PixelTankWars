package de.htwg.se.msiwar.aview.db.model

import slick.jdbc.H2Profile.api._

case class GameObject(gameConfigId: Int, name: String, id: Option[Int] = None)

class GameObjectTable(tag: Tag) extends Table[GameObject](tag, "GAMEOBJECTS") {
  // Auto Increment the id primary key column
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def gameConfigId = column[Int]("GAME_CONF_ID")
  def name = column[String]("NAME")

  // the * projection (e.g. select * ...) auto-transforms the tupled
  //    // column values to / from a GameConfig
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (gameConfigId, name, id.?) <> (GameObject.tupled, GameObject.unapply)

  def gameConfig = foreignKey("game_config_fk", gameConfigId, TableQuery[GameConfigTable])(_.id, onDelete = ForeignKeyAction.Cascade)
}
