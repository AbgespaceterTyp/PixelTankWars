package de.htwg.ptw.generator.rest

import akka.http.scaladsl.server.Directives._
import de.htwg.ptw.generator.GameGeneratorApp

object Routes {
  val gameGenerator = new GameGeneratorApp

  val all =
  get {
      pathPrefix("generate" / IntNumber / IntNumber ) { ( rowIndex, columnIndex) =>
        complete {
          gameGenerator.generate(rowIndex, columnIndex)
          "Generating Levels..."
        }
      }
    }
}
