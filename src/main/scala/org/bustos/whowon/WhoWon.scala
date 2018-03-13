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
import com.github.tototoshi.csv._
import com.typesafe.config.ConfigFactory
import org.bustos.whowon.WhoWonTables.Bracket
import spray.can.Http

import scala.concurrent.duration._
import scala.util.Properties.envOrElse
import WhoWonData._
import WhoWonTables._

import scala.slick.driver.MySQLDriver.simple._

object WhoWon extends App {

  def doMain = {

    implicit val system = ActorSystem()
    implicit val timeout = Timeout(DurationInt(5).seconds)

    val config = ConfigFactory.load
    val portFromEnv = envOrElse("PORT", "") != ""
    val port = envOrElse("PORT", config.getString("server.port"))

    if (!portFromEnv) initializeData

    val server = system.actorOf(Props[WhoWonServiceActor], "whowonRoutes")

    if (args.length > 0) IO(Http) ? Http.Bind(server, "0.0.0.0", args(0).toInt)
    else IO(Http) ? Http.Bind(server, "0.0.0.0", port.toInt)
  }

  def importBrackets(year: Int) = {

    db.withSession { implicit session =>
      bracketsTable.filter(_.year === year).delete
      val reader = CSVReader.open(new File("data/" + year + "/brackets.csv"))
      reader.allWithHeaders.foreach(fields => {
        println(fields)
        val timestamp = {
          try {
            formatter.parseDateTime(fields("gameTime"))
          } catch {
            case _: Exception => ccyyFormatter.parseDateTime(fields("gameTime"))
          }
        }
        bracketsTable += Bracket(fields("bookId").toInt, fields("opposingBookId").toInt, fields("year").toInt,
          fields("region"), fields("seed").toInt, fields("teamName"), timestamp,
          fields("firstHalf").toInt, fields("secondHalf").toInt, fields("firstTo15").toInt,
          fields("opposingFirstHalf").toInt, fields("opposingSecondHalf").toInt, fields("opposingFirstTo15").toInt)
      })
    }
  }

  def importResults(year: Int) = {

    db.withSession { implicit session =>
      resultsTable.filter(_.year === year).delete
      val reader = CSVReader.open(new File("data/" + year + "/results.csv"))
      reader.allWithHeaders.filter { fields => !fields("year").isEmpty } foreach(fields => {
        println(fields)
        val timestamp = {
          try {
            formatter.parseDateTime(fields("resultTimeStamp"))
          } catch {
            case _: Exception => ccyyFormatter.parseDateTime(fields("resultTimeStamp"))
          }
        }
        resultsTable += GameResult(fields("year").toInt,
          fields("bookId").toInt, fields("finalScore").toInt, fields("firstHalfScore").toInt,
          fields("opposingBookId").toInt, fields("opposingFinalScore").toInt, fields("opposingFirstHalfScore").toInt,
          fields("firstTo15").toBoolean, timestamp)
      })
    }
  }

  def initializeData = {

    db.withSession { implicit session =>
      playersTable.delete
      val reader = CSVReader.open(new File("data/whoWonPlayers.csv"))
      reader.foreach(fields => {
        println(fields)
        playersTable += Player(fields(0).toInt, fields(1), fields(2), fields(3), fields(4))
      })
    }

    //importBrackets(2015)
    //importResults(2015)
    //importBrackets(2016)
    //importResults(2016)
    //importBrackets(2017)
    //importResults(2017)
    importBrackets(2018)
  }

  doMain

}
