package de.htwg.se.msiwar.aview.db

import slick.driver.H2Driver.api._

case class GameConfig(name: String, id: Option[Int] = None) {}

class GameConfigTable(tag: Tag) extends Table[GameConfig](tag, "GAMECONFIGS") {
    // Auto Increment the id primary key column
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    // The name can't be null
    def name = column[String]("NAME")
    // the * projection (e.g. select * ...) auto-transforms the tupled
    // column values to / from a GameConfig
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (name, id.?) <> (GameConfig.tupled, GameConfig.unapply)
}
