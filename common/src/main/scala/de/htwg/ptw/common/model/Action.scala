package de.htwg.ptw.common.model

import de.htwg.ptw.common.ActionType.ActionType

case class Action(id: Int, description: String, imagePath: String, soundPath: String, actionPoints: Int, range: Int, actionType: ActionType, damage: Int) {
}
