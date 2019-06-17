package de.htwg.se.msiwar.aview

import com.google.inject.AbstractModule
import de.htwg.se.msiwar.db.{BaseDao, MongoDbGameConfigDao}
import net.codingwell.scalaguice.ScalaModule

class MainAppModule extends AbstractModule with ScalaModule {

  def configure(): Unit = {
    bind[BaseDao].toInstance(new MongoDbGameConfigDao)
    // NOTE: Comment in when you want to use slick instead of mongo db
    //bind[BaseDao].toInstance(new SlickGameConfigDao)
  }
}
