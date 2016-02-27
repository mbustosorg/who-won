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

import java.io.{FileOutputStream, File}

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import sun.misc.BASE64Decoder
import sun.plugin.dom.exception.InvalidStateException
import scala.collection.immutable.Iterable
import scala.util.Properties.envOrElse
import scala.slick.driver.MySQLDriver.simple._
import org.joda.time._
import scala.concurrent.duration._
import spray.json._

object WhoWonData {

  val quarterMile = 1.0 / 60.0 / 4.0 // In degrees
  val TicketImageDestination = "src/main/resources/webapp/tickets/"
  val WestCoastId = "America/Los_Angeles"
  val hhmmssFormatter = DateTimeFormat.forPattern("hh:mm:ss a")
  val filedateFormatter = DateTimeFormat.forPattern("yyyymmddhhmmss")

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
    db.withSession { implicit session =>
      playersTable.list
    }.map({ x => (x.userName, x)}).toMap
  }

  val playersById = {
    db.withSession { implicit session =>
      playersTable.list
    }.map({ x => (x.id, x)}).toMap
  }

  val bookIds = {
    db.withSession { implicit session =>
      bracketsTable.list
    }.map({ x =>
      ((x.bookId, x.year), x)
    }).toMap
  }

  def validPlayer(playerId: String): Boolean = players.contains(playerId)

  def validBookId(bookId: Int, year: Int): Boolean = bookIds.contains((bookId, year))

  def playerIdFromUserName(userName: String): Int = players(userName).id

  def userNameFromPlayerId(id: Int): String = playersById(id).userName

  def betResult(bet: Bet, gameResult: Option[GameResult]): BetDisplay = {
    val bracket = bookIds((bet.bookId, bet.year))
    gameResult match {
      case Some(result) =>
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
            if (resultType == "Win") bet.amount + bet.amount * StraightBetPayoff
            else if (resultType == "Lose") 0.0
            else bet.amount
          } else {
            if (bet.spread_ml >= 100.0) {
              bet.amount + bet.amount * (bet.spread_ml / 100.0)
            } else {
              bet.amount + bet.amount * (1.0 / (bet.spread_ml / 100.0))
            }
          }
        }
        BetDisplay(bet, bracket, winnings.toDouble, resultType)
      case None => BetDisplay(bet, bracket, 0.0, "Not yet")
    }
  }

  def receive = {
    case bet: Bet =>
      if (!validPlayer(bet.userName)) sender ! UnknownPlayer
      else if (!validBookId(bet.bookId, bet.year)) sender ! UnknownBookId
      else {
        val message = db.withSession { implicit session =>
          val previousBet = betsTable.filter({ x => x.userName === bet.userName && x.bookId === bet.bookId && x.betType === bet.betType }).list
          if (previousBet.nonEmpty) {
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
    case BetsRequest(userName, year) =>
      if (validPlayer(userName)) {
        val gameResults = db.withSession { implicit session =>
          resultsTable.filter(_.year === year).sortBy(_.resultTimeStamp).list.map({ x => (x.bookId, x) }).toMap
        }
        val bets = db.withSession { implicit session =>
          betsTable.filter({ x => x.userName === userName && x.year === year }).list
        }.map({ bet =>
          if (gameResults.contains(bet.bookId)) betResult(bet, Some(gameResults(bet.bookId)))
          else betResult(bet, None)
        })
        sender ! Bets(bets)
      } else sender ! UnknownPlayer
    case result: GameResult =>
      db.withSession { implicit session =>
        resultsTable += result
        resultsTable += GameResult(result.year, result.opposingBookId, result.opposingScore, result.bookId, result.score, result.resultTimeStamp)
      }
      sender ! ResultSubmitted
    case MissingGameResultsRequest(year) =>
      val gameResults = db.withSession { implicit session =>
        (for {
          (c, s) <- bracketsTable leftJoin resultsTable on (_.bookId === _.bookId)
        } yield (c.bookId, c.year, c.region, c.seed, c.teamName, c.gameTime, s.bookId.?))
          .filter(_._7.isEmpty)
          .sortBy(_._1)
          .list
          .map({ x => Bracket(x._1, x._2, x._3, x._4, x._5, x._6) })
      }
      sender ! BookIdsResults(gameResults)
    case GameResultsRequest(year) =>
      val gameResults = db.withSession { implicit session =>
        (for {
          c <- bracketsTable if c.year === year
          s <- resultsTable if c.bookId === s.bookId && s.bookId < s.opposingBookId
          d <- bracketsTable if s.opposingBookId === d.bookId
        } yield (c.bookId, d.bookId, c.seed, d.seed, c.teamName, d.teamName, s.score, s.opposingScore, s.resultTimeStamp))
          .sortBy(_._7.desc)
          .list
        .map({ x => GameResultDisplay(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9) })
      }
      sender ! GameResults(gameResults)
    case WinningsTrackRequest(year) =>
      val gameResults = db.withSession { implicit session =>
        resultsTable.filter(_.year === year).sortBy(_.resultTimeStamp).list.map({ x => (x.bookId, x) }).toMap
      }
      val bets = db.withSession { implicit session =>
        betsTable.filter(_.year === year).list.groupBy(_.userName)
      }
      val outlays = db.withSession { implicit session =>
        betsTable.groupBy(_.userName).map({ case (user, bets) => (user, bets.map(_.amount).sum)}).list.toMap
      }
      val resultsTimestamps: List[DateTime] = gameResults.map({ case (k, v) => v.resultTimeStamp }).toList.distinct.sorted
      val timestamps = { new DateTime(resultsTimestamps.head).plusMinutes(-15) } :: resultsTimestamps
      val acc = bets.map({ case (k, v) =>
        var outlay = -outlays(k).get
        (k, timestamps.map({ timestamp =>
          v.foldLeft((outlay, 0.0, 0))({ (acc, x) =>
            if (gameResults.contains(x.bookId)) {
              val game = gameResults(x.bookId)
              if (game.resultTimeStamp.getMillis <= timestamp.getMillis) {
                val payoff = betResult(x, Some(game)).payoff
                val win = if (payoff > 0.0) 1 else 0
                (acc._1 + payoff, acc._2 + win, acc._3 + 1)
              } else acc
            } else acc
          })
        }))
      }).map({ case (k, v) => PlayerWinnings(k, v.map({ x => x._1}), v.map({ x => if (x._3 > 0) (x._2 / x._3 * 100.0).toInt else 0}))})
      val tracking = WinningsTrack(timestamps, acc.toList)
      sender ! tracking
    case BookIdsRequest(year) =>
      val bookIdResults = db.withSession { implicit session =>
        bracketsTable.filter(_.year === year).list
      }
      sender ! BookIdsResults(bookIdResults)
    case PlayerIdRequest(name) =>
      val player = db.withSession { implicit session =>
        playersTable.filter(_.userName === name).list
      }
      if (player.isEmpty) sender ! UnknownPlayer
      else sender ! player.head
    case TicketImage(name, image) =>
      val decodedString = image.decodeString("ISO_8859_1").split(',').tail.head
      val directory = new File(TicketImageDestination + name)
      if (!directory.exists) directory.mkdir
      val dateString = filedateFormatter.print(new DateTime)
      val decodedFile = new File(TicketImageDestination + name + "/ticket_" + dateString + ".png")
      val decoded = new BASE64Decoder().decodeBuffer(decodedString)
      val decodedStream = new FileOutputStream(decodedFile)
      decodedStream.write(decoded)
      decodedStream.close()
      sender ! "tickets/" + name + "/ticket_" + dateString + ".png"
  }
}
