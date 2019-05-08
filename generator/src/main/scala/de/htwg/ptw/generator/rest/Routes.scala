package de.htwg.ptw.generator.rest

import akka.http.scaladsl.server.Directives._
import de.htwg.ptw.generator.GameGenerator

object Routes {

  val all =
    get {
      pathPrefix("commands" / IntNumber / IntNumber ) { ( rowIndex, columnIndex) =>
        complete {
          "" + GameGenerator(rowIndex, columnIndex)
        }
      }
    }
}
