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

  val players = {
    (db.withSession { implicit session =>
      playersTable.list
    }).map({ x => (x.userName, x)}).toMap
  }

  val playersById = {
    (db.withSession { implicit session =>
      playersTable.list
    }).map({ x => (x.id, x)}).toMap
  }

  val bookIds = {
    (db.withSession { implicit session =>
      bracketsTable.list
    }).map({ x => ((x.bookId, x.year), x)}).toMap
  }

  def validPlayer(playerId: String): Boolean = players.contains(playerId)

  def validBookId(bookId: Int, year: Int): Boolean = bookIds.contains((bookId, year))

  def playerIdFromUserName(userName: String): Int = players(userName).id

  def userNameFromPlayerId(id: Int): String = playersById(id).userName

  def betResult(bet: Bet): BetDisplay = {
    val gameResult = db.withSession { implicit session =>
      resultsTable.filter({ x => x.year === x.year && x.bookId === bet.bookId }).list
    }
    val bracket = bookIds((bet.bookId, bet.year))
    if (gameResult.isEmpty) {
      BetDisplay(bet, bracket, (0.0).toFloat, "Not yet")
    } else {
      val result = gameResult.head
      val score = {
        if (bet.betType == StraightBet) {
          result.score - result.opposingScore + bet.spread_ml
        } else {
          result.score - result.opposingScore + bet.spread_ml
        }
      }
      val resultType = {
        if (score > 0) "Win"
        else if (score < 0) "Lose"
        else "Push"
      }
      val winnings = {
        if (bet.betType == StraightBet) {
          bet.amount + bet.amount * StraightBetPayoff
        } else {
          if (bet.spread_ml >= 100.0) {
            bet.amount + bet.amount * (bet.spread_ml / 100.0)
          } else {
            bet.amount + bet.amount * (1.0 / (bet.spread_ml / 100.0))
          }
        }
      }
      BetDisplay(bet, bracket, winnings.toFloat, resultType)
    }
  }

  def receive = {
    case bet: Bet => {
      if (!validPlayer(bet.userName)) sender ! UnknownPlayer
      else if (!validBookId(bet.bookId, bet.year)) sender ! UnknownBookId
      else {
        val message = db.withSession { implicit session =>
          val previousBet = betsTable.filter({ x => x.userName === bet.userName && x.bookId === bet.bookId && x.betType === bet.betType }).list
          if (!previousBet.isEmpty) {
            betsTable.filter({ x => x.userName === bet.userName && x.bookId === bet.bookId && x.betType === bet.betType }).delete
            betsTable += bet
            BetReplaced
          } else {
            betsTable += bet
            BetSubmitted
          }
        }
        sender ! message
      }
    }
    case BetsRequest(userName, year) => {
      if (validPlayer(userName)) {
        val bets = db.withSession { implicit session =>
          betsTable.filter({ x => x.userName === userName && x.year === year }).list
        }.map({ bet =>
          betResult(bet)
        })
        sender ! Bets(bets)
      } else sender ! UnknownPlayer
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
    case BookIdsRequest(year) => {
      val bookIdResults = db.withSession { implicit session =>
        bracketsTable.filter(_.year === year).list
      }
      sender ! BookIdsResults(bookIdResults)
    }
    case PlayerIdRequest(name) => {
      val player = db.withSession { implicit session =>
        playersTable.filter(_.userName === name).list
      }
      if (player.isEmpty) sender ! UnknownPlayer
      else sender ! player.head
    }
  }
}
