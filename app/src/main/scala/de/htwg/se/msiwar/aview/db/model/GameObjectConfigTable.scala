package de.htwg.se.msiwar.aview.db.model

import de.htwg.ptw.common.model.{BlockObject, GameObject, PlayerObject}
import slick.jdbc.H2Profile.api._

case class GameObjectConfig(gameConfigId: Int, name: String, imagePath: String, positionRow: Int, positionCol: Int,
                            viewDirection: String, playerNumber: Int, wonImagePath: String, actionPoints: Int,
                            maxActionPoints: Int, healthPoints: Int, maxHealthPoints: Int, id: Option[Int] = None){

  def this(gameConfigId: Int, pObj: PlayerObject) = this(gameConfigId, pObj.name, pObj.imagePath, pObj.position.rowIdx,
    pObj.position.columnIdx, pObj.viewDirection.toString, pObj.playerNumber, pObj.wonImagePath, pObj.actionPoints,
    pObj.maxActionPoints, pObj.healthPoints, pObj.maxHealthPoints)

  def this(gameConfigId: Int, bObj: BlockObject) = this(gameConfigId, bObj.name, bObj.imagePath, bObj.position.rowIdx,
    bObj.position.columnIdx, "", -1, "", 0, 0, 0, 0)

  def this(gameConfigId: Int, gObj: GameObject) = this(gameConfigId, gObj.name, gObj.imagePath, gObj.position.rowIdx,
    gObj.position.columnIdx, "", -1, "", 0, 0, 0, 0)
}

class GameObjectConfigTable(tag: Tag) extends Table[GameObjectConfig](tag, "GAMEOBJECTS") {
  // Auto Increment the id primary key column
  def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)
  def gameConfigId = column[Int]("GAME_CONF_ID")
  def name = column[String]("NAME")
  def imagePath = column[String]("IMG_PATH")
  def positionRow = column[Int]("POS_ROW")
  def positionCol = column[Int]("POS_COL")
  def viewDirection = column[String]("VIEW_DIR")
  def playerNumber= column[Int]("PLAYER_NR")
  def wonImagePath = column[String]("WON_IMG_PATH")
  def actionPoints = column[Int]("ACT_POINTS")
  def maxActionPoints = column[Int]("MAX_ACT_POINTS")
  def healthPoints = column[Int]("HLTH_POINTS")
  def maxHealthPoints = column[Int]("MAX_HLTH_POINTS")

  // the * projection (e.g. select * ...) auto-transforms the tupled
  //    // column values to / from a GameConfig
  // Every table needs a * projection with the same type as the table's type parameter
  def * = (gameConfigId, name, imagePath, positionRow, positionCol, viewDirection, playerNumber,
    wonImagePath, actionPoints, maxActionPoints, healthPoints, maxHealthPoints, id.?) <> (GameObjectConfig.tupled, GameObjectConfig.unapply)

  def gameConfig = foreignKey("game_config_fk", gameConfigId, TableQuery[GameConfigTable])(_.id, onDelete = ForeignKeyAction.Cascade)
}
