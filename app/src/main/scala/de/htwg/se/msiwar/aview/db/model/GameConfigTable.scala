package de.htwg.se.msiwar.aview.db.model

import slick.jdbc.H2Profile.api._

case class GameConfig(attackSoundPath: String, openingBackgroundImagePath: String, levelBackgroundImagePath: String,
                      actionbarBackgroundImagePath:String, attackImagePath: String, appIconImagePath: String,
                      rowCount: Int, colCount: Int, id: Option[Int] = None)

class GameConfigTable(tag: Tag) extends Table[GameConfig](tag, "GAMECONFIGS") {
    // Auto Increment the id primary key column
    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
    def attackSoundPath = column[String]("ATT_SOUND_PATH")
    def openingBackgroundImagePath = column[String]("OP_IMG_PATH")
    def levelBackgroundImagePath = column[String]("LVL_IMG_PATH")
    def actionbarBackgroundImagePath = column[String]("ACT_IMG_PATH")
    def attackImagePath = column[String]("ATT_IMG_PATH")
    def appIconImagePath = column[String]("APP_IMG_PATH")
    def rowCount = column[Int]("ROW_CNT")
    def colCount = column[Int]("COL_CNT")

    // the * projection (e.g. select * ...) auto-transforms the tupled
    //    // column values to / from a GameConfig
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (attackSoundPath, openingBackgroundImagePath, levelBackgroundImagePath,
      actionbarBackgroundImagePath, attackImagePath, appIconImagePath,
      rowCount, colCount, id.?) <> (GameConfig.tupled, GameConfig.unapply)
}
