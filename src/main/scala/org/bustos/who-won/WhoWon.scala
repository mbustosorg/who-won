/*

    Copyright (C) 2016 Mauricio Bustos (m@bustos.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.bustos.whowon

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.joda.time.{DateTimeZone, DateTime}
import spray.can.Http
import com.typesafe.config.ConfigFactory
import scala.util.Properties.envOrElse
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tototoshi.csv._

object WhoWon extends App {

  def doMain = {

    implicit val system = ActorSystem()
    implicit val timeout = Timeout(DurationInt(5).seconds)

    initializeData

    val server = system.actorOf(Props[WhoWonServiceActor], "whowonRoutes")
    val config = ConfigFactory.load

    val port = envOrElse("PORT", config.getString("server.port"))

    if (args.length > 0) IO(Http) ? Http.Bind(server, "0.0.0.0", args(0).toInt)
    else {
      IO(Http) ? Http.Bind(server, "0.0.0.0", port.toInt)
    }
  }

  def initializeData = {
    import WhoWonTables._
    import WhoWonData._
    import scala.slick.driver.MySQLDriver.simple._

    db.withSession { implicit session =>
      playersTable.delete
      val reader = CSVReader.open(new File("data/whoWonPlayers.csv"))
      reader.foreach(fields => {
        println(fields)
        playersTable += Player(fields(0).toInt, fields(1), fields(2), fields(3), fields(4))
      })
    }
    db.withSession { implicit session =>
      bracketsTable.filter(_.year === 2015).delete
      val reader = CSVReader.open(new File("data/2015brackets.csv"))
      reader.foreach(fields => {
        println(fields)
        if (fields(0) != "bookId") {
          bracketsTable += Bracket(fields(0).toInt, fields(1).toInt, fields(2), fields(3).toInt, fields(4), formatter.parseDateTime(fields(5)))
        }
      })
    }
    db.withSession { implicit session =>
      resultsTable.filter(_.year === 2015).delete
      val reader = CSVReader.open(new File("data/2015results.csv"))
      reader.foreach(fields => {
        println(fields)
        if (fields(0) != "bookId") {
          resultsTable += GameResult(fields(0).toInt, fields(1).toInt, fields(2).toInt, fields(3).toInt, formatter.parseDateTime(fields(4)))
        }
      })
    }
  }

  doMain

}
