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

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import scala.util.Properties.envOrElse
import scala.slick.driver.MySQLDriver.simple._
import org.joda.time._
import scala.concurrent.duration._
import spray.json._

object WhoWonData {

  val quarterMile = 1.0 / 60.0 / 4.0 // In degrees
  val WestCoastId = "America/Los_Angeles"
  val hhmmssFormatter = DateTimeFormat.forPattern("hh:mm:ss a")

  val db = {
    val mysqlURL = envOrElse("WHOWON_MYSQL_URL", "jdbc:mysql://localhost:3306/whowon")
    val mysqlUser = envOrElse("WHOWON_MYSQL_USER", "root")
    val mysqlPassword = envOrElse("WHOWON_MYSQL_PASSWORD", "")
    Database.forURL(mysqlURL, driver = "com.mysql.jdbc.Driver", user = mysqlUser, password = mysqlPassword)
  }
}

class WhoWonData extends Actor with ActorLogging {

  import WhoWonData._
  import WhoWonTables._
  import WhoWonJsonProtocol._

  val logger =  LoggerFactory.getLogger(getClass)

  implicit val defaultTimeout = Timeout(1 seconds)

  def receive = {
    case Bet(id, playerId, bookId, spread, amount) => {
      db.withSession { implicit session =>
        betsTable += Bet(id, playerId, bookId, spread, amount)
      }
      sender ! Bet(id, playerId, bookId, spread, amount)
    }
    case BetsRequest(playerId) => {
      val bets = db.withSession { implicit session =>
        betsTable.filter(_.playerId === playerId).list
      }.map({ bet =>
        (bet, true)
      })
      sender ! Bets(bets)
    }
    case x: GameResult => {
      db.withSession { implicit session =>
        resultsTable += x
      }
      sender ! "success"
    }
    case GameResultsRequest(year) => {
      val gameResults = db.withSession { implicit session =>
        resultsTable.filter(_.year === year).list
      }
      sender ! GameResults(gameResults)
    }
  }
}
