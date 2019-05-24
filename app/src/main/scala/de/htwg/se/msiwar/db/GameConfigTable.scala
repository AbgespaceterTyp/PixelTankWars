package de.htwg.se.msiwar.db

import slick.jdbc.H2Profile.api._

case class GameConfig(name: String, config: String, id: Option[Int] = None)

class GameConfigTable(tag: Tag) extends Table[GameConfig](tag, "GAMECONFIGS") {
    // Auto Increment the id primary key column
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def config = column[String]("CONFIG")

    // the * projection (e.g. select * ...) auto-transforms the tupled
    //    // column values to / from a GameConfig
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (name, config, id.?) <> (GameConfig.tupled, GameConfig.unapply)
}
