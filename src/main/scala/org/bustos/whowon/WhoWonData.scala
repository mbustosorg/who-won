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

import java.io.{File, FileOutputStream}

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import com.github.tototoshi.csv.CSVReader
import org.bustos.whowon.ImageResize.resize
import org.joda.time._
import org.joda.time.format.DateTimeFormat
import org.slf4j.LoggerFactory
import sun.misc.BASE64Decoder

import scala.collection.immutable.Iterable
import scala.concurrent.duration._
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.StaticQuery.interpolation
import scala.util.Properties.envOrElse

object WhoWonData {

  import WhoWonTables._

  val logger =  LoggerFactory.getLogger(getClass)

  val quarterMile = 1.0 / 60.0 / 4.0 // In degrees
  val TicketImageDestination = "src/main/resources/webapp/tickets/"
  val WestCoastId = "America/Los_Angeles"
  val LocalTimeZone = DateTimeZone.forID(WestCoastId)
  val hhmmssFormatter = DateTimeFormat.forPattern("hh:mm:ss a")
  val filedateFormatter = DateTimeFormat.forPattern("yyyyMMdd_HHmmss")

  val db = {
    val mysqlURL = envOrElse("WHOWON_MYSQL_URL", "jdbc:mysql://localhost:3306/whowon")
    val mysqlUser = envOrElse("WHOWON_MYSQL_USER", "root")
    val mysqlPassword = envOrElse("WHOWON_MYSQL_PASSWORD", "")
    Database.forURL(mysqlURL, driver = "com.mysql.jdbc.Driver", user = mysqlUser, password = mysqlPassword)
  }

  val devDb = {
    envOrElse("WHOWON_MYSQL_URL", "jdbc:mysql://localhost:3306/whowon").contains("_dev")
  }

  val s3 = new AmazonS3Client
  val usWest2 = Region.getRegion(Regions.US_WEST_2)
  val S3bucket = "who-won-tickets"

  def importBrackets(year: Int) = {

    db.withSession { implicit session =>
      bracketsTable.filter(_.year === year).delete
      val reader = CSVReader.open(new File("data_dev/" + year + "_dev/brackets.csv"))
      //val reader = CSVReader.open(new File("data/" + year + "/brackets.csv"))
      reader.allWithHeaders.foreach(fields => {
        logger.info(fields.toString)
        val timestamp = {
          try {
            formatter.parseDateTime(fields("gameTime"))
          } catch {
            case _: Exception => ccyyFormatter.parseDateTime(fields("gameTime"))
          }
        }
        try {
          bracketsTable += WhoWonTables.Bracket(fields("bookId").toInt, fields("opposingBookId").toInt, fields("year").toInt,
            fields("region"), fields("seed").toInt, fields("teamName"), timestamp,
            fields("firstHalf").toInt, fields("secondHalf").toInt, fields("firstTo15").toInt,
            fields("opposingFirstHalf").toInt, fields("opposingSecondHalf").toInt, fields("opposingFirstTo15").toInt)
        } catch {
          case _: Exception => ccyyFormatter.parseDateTime(fields("gameTime"))
        }
      })
    }
  }

  def importResults(year: Int) = {

    db.withSession { implicit session =>
      resultsTable.filter(_.year === year).delete
      val reader = CSVReader.open(new File("data_dev/" + year + "_dev/results.csv"))
      reader.allWithHeaders.filter { fields => !fields("year").isEmpty } foreach(fields => {
        logger.info(fields.toString)
        val timestamp = {
          try {
            formatter.parseDateTime(fields("resultTimeStamp"))
          } catch {
            case _: Exception => ccyyFormatter.parseDateTime(fields("resultTimeStamp"))
          }
        }
        resultsTable += WhoWonTables.GameResult(fields("year").toInt,
          fields("bookId").toInt, fields("finalScore").toInt, fields("firstHalfScore").toInt,
          fields("opposingBookId").toInt, fields("opposingFinalScore").toInt, fields("opposingFirstHalfScore").toInt,
          fields("firstTo15").toBoolean, timestamp)
      })
    }
  }

  def initializeData = {

    db.withSession { implicit session =>
      betsTable.delete
      playersTable.delete
      val reader = CSVReader.open(new File("data_dev/whoWonPlayers.csv"))
      //val reader = CSVReader.open(new File("data/whoWonPlayers.csv"))
      reader.foreach(fields => {
        logger.info(fields.toString)
        playersTable += Player(fields(0).toInt, fields(1), fields(2), fields(3), fields(4))
      })
    }
  }

}

class WhoWonData extends Actor with ActorLogging {

  import WhoWonData._
  import WhoWonTables._

  val logger =  LoggerFactory.getLogger("who-won")

  val ocrApi = new OcrAPI

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
            result.finalScore - result.opposingFinalScore + bet.spread_ml
          } else if (bet.betType == MoneylineBet) {
            result.finalScore - result.opposingFinalScore
          } else if (bet.betType == FirstTo15Moneyline) {
            if (result.firstTo15) 1 else -1
          } else if (bet.betType == FirstHalfMoneyline) {
            result.firstHalfScore - result.opposingFirstHalfScore
          } else if (bet.betType == Over) {
            result.finalScore + result.opposingFinalScore - bet.spread_ml
          } else if (bet.betType == Under) {
            bet.spread_ml - (result.finalScore + result.opposingFinalScore)
          } else 0
        }
        val resultType = {
          if (score > 0) "Win"
          else if (score < 0) "Lose"
          else "Push"
        }
        val winnings = {
          if (bet.betType == StraightBet || bet.betType == Over || bet.betType == Under) {
            if (resultType == "Win") bet.amount + bet.amount * StraightBetPayoff
            else if (resultType == "Lose") 0.0
            else bet.amount
          } else {
            if (resultType == "Lose") 0.0
            else {
              if (bet.spread_ml >= 100.0) {
                bet.amount + bet.amount * (bet.spread_ml / 100.0)
              } else {
                bet.amount + bet.amount * (1.0 / (bet.spread_ml / 100.0))
              }
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
          val previousBet = betsTable.filter({ x => x.userName === bet.userName && x.bookId === bet.bookId && x.betType === bet.betType && x.year === bet.year}).list
          if (previousBet.nonEmpty) {
            betsTable.filter({ x => x.userName === bet.userName && x.bookId === bet.bookId && x.betType === bet.betType && x.year === bet.year }).delete
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
        resultsTable += GameResult(result.year, result.opposingBookId, result.opposingFinalScore, result.opposingFirstHalfScore,
          result.bookId, result.finalScore, result.firstHalfScore, result.firstTo15, result.resultTimeStamp)
      }
      sender ! ResultSubmitted
    case MissingGameResultsRequest(year) =>
      val gameResults = db.withSession { implicit session =>
        (for {
          (c, s) <- bracketsTable.filter(_.year === year) leftJoin resultsTable.filter(_.year === year) on (_.bookId === _.bookId)
        } yield (c.bookId, c.opposingBookId, c.year, c.region, c.seed, c.teamName, c.gameTime, s.bookId.?,
          c.firstHalf, c.secondHalf, c.firstTo15, c.opposingFirstHalf, c.opposingSecondHalf, c.opposingFirstTo15))
          .filter(_._8.isEmpty)
          .sortBy(_._1)
          .list
          .map({ x => Bracket(x._1, x._2, x._3, x._4, x._5, x._6, x._7.toDateTime(LocalTimeZone),
            x._9, x._10, x._11, x._12, x._13, x._14)})
      }
      sender ! BookIdsResults(gameResults)
    case GameResultsRequest(year) =>
      val gameResults = db.withSession { implicit session =>
        (for {
          c <- bracketsTable if c.year === year
          s <- resultsTable if c.bookId === s.bookId && s.bookId < s.opposingBookId && s.year === year
          d <- bracketsTable if s.opposingBookId === d.bookId && d.year === year
        } yield (c.bookId, d.bookId, c.seed, d.seed, c.teamName, d.teamName,
          s.finalScore, s.opposingFinalScore, s.firstHalfScore, s.opposingFirstHalfScore, s.resultTimeStamp, s.firstTo15))
          .sortBy(_._1.desc)
          .list
        .map({ x =>
          GameResultDisplay(x._1, x._2, x._3, x._4, x._5, x._6, x._7, x._8, x._9, x._10, x._12, !x._12, x._11.toDateTime(LocalTimeZone))
        })
      }
      sender ! GameResults(gameResults)
    case WinningsTrackRequest(year) =>
      val gameResults = db.withSession { implicit session =>
        resultsTable.filter(_.year === year).sortBy(_.resultTimeStamp).list.map({ x => (x.bookId, x) }).toMap
      }
      val bets = db.withSession { implicit session =>
        betsTable.filter(_.year === year).sortBy({ x => (x.userName, x.timestamp) }).list.groupBy(_.userName)
      }
      val betsByUserMap: Map[String, Map[String, Bet]] = bets.map({ case (k, v) => (k, v.map({ x => ((x.bookId + x.betType), x)}).toMap)})
      val resultsTimestamps: List[DateTime] = gameResults.map({ case (k, v) => v.resultTimeStamp }).toList.distinct.sorted
      val betTimestamps = bets.map({ case (k, v) => v.map(_.timestamp)}).flatMap(x => x).toList.distinct.sorted
      val timestamps: List[DateTime] = (resultsTimestamps ++ betTimestamps).distinct.sorted

      def accumulateOutlay(user: String, acc: (Double, Int, Int, Double, Double, Double), bet: Bet, betType: String): (Double, Int, Int, Double, Double, Double) = {
        // Wallet, win count, bet count, investment, winnings, live winnings
        if (betsByUserMap(user).contains(betType) && betsByUserMap(user)(betType).timestamp.getMillis == bet.timestamp.getMillis)
          (acc._1 - betsByUserMap(user)(betType).amount, acc._2, acc._3, acc._4 - betsByUserMap(user)(betType).amount, acc._5, acc._6)
        else acc
      }

      def bookTypeList(bet: Bet): List[String] = {
        List(bet.bookId + "ML", bet.bookId + "ST", bet.bookId + "ST-OV", bet.bookId + "ST-UN", bet.bookId + "ML-15", bet.bookId + "ML-1H")
      }

      val acc: Iterable[PlayerWinnings] = bets.map({ case (k, v) =>
        (k, timestamps.map({ timestamp =>
          val timeMillis = timestamp.getMillis
          val filteredBets = v.filter(_.timestamp.getMillis <= timeMillis)
          filteredBets.foldLeft((0.0, 0, 0, 0.0, 0.0, 0.0))({ (acc, x) =>
            val newAcc = bookTypeList(x).foldLeft(acc)({ (acc, betType) => accumulateOutlay(k, acc, x, betType) })
            if (gameResults.contains(x.bookId)) {
              val game = gameResults(x.bookId)
              if (game.resultTimeStamp.getMillis <= timeMillis) {
                val currentResult = betResult(x, Some(game))
                val payoff = currentResult.payoff
                val amount = currentResult.bet.amount
                val win = if (payoff > 0.0) 1 else 0
                (newAcc._1 + payoff, newAcc._2 + win, newAcc._3 + 1, newAcc._4, newAcc._5 + payoff, newAcc._6 - amount + payoff) // Wallet, win count, bet count, investment, winnings, live winnings
              } else newAcc
            } else newAcc
          })
        }))
      }).map({ case (k, v) =>
        PlayerWinnings(k,                                                            // Username
          v.map({ x => x._1 }),                                                      // Wallet
          v.map({ x => if (x._3 > 0) (x._2 / x._3 * 100.0).toInt else 0}),           // Percentage
          v.map({ x => if (x._3 > 0) (x._5 + x._4) / x._4.abs else 0.0}),            // ROI
          v.map({ x => x._6 }))}                                                     // ROI Live
      )
      val tracking = WinningsTrack(timestamps.map({ ts =>
        val tsLocal = ts.minusHours(7)
        if (tsLocal.getDayOfYear > timestamps.head.getDayOfYear) tsLocal.minusHours(10)
        else tsLocal
      }), acc.toList)
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
      logger.info("Computing small ticket image for " + name)
      val decodedString = image.decodeString("ISO_8859_1").replaceAll("\"", "")

      val destinationDirectory = new File(TicketImageDestination)
      if (!destinationDirectory.exists) destinationDirectory.mkdir
      val directory = new File(TicketImageDestination + name)
      if (!directory.exists) directory.mkdir

      val dateString = filedateFormatter.print(new DateTime)
      val filePath = TicketImageDestination + name + "/ticket_" + dateString + ".png"

      val decodedFile = new File(filePath)
      val decoded = new BASE64Decoder().decodeBuffer(decodedString)
      val decodedStream = new FileOutputStream(decodedFile)
      decodedStream.write(decoded)
      decodedStream.close()
      val smallName = filePath.replaceAll(".jpg", "_small.jpg").replaceAll(".png", "_small.png").replaceAll("original", "resized")
      val newImage = resize(filePath, smallName, 0.4)
      logger.info("Complete")
      val bet = ocrApi.detectedBet(smallName)
      sender ! bet._1
      val key = bet._1.year.toString + "/" + name + "/ticket_" + dateString + ".png"
      s3.putObject(new PutObjectRequest(S3bucket, key, new File(smallName)))
      val textKey = bet._1.year.toString + "/" + name + "/ticket_" + dateString + ".txt"
      s3.putObject(new PutObjectRequest(S3bucket, textKey, ocrApi.ticketTextFile(smallName)))
    case BetProfilesRequest(year) =>
      val betCounts: List[(String, Double, Int)] = db.withSession { implicit session =>
        betsTable
          .filter(_.year === year)
          .groupBy({ row => (row.userName, row.amount) })
          .map({ case ((rowName, amount), results) => (rowName, amount, results.length) })
          .list
      }.sortBy(_._2)
      val lowBets = {
        if (betCounts.isEmpty) List()
        else betCounts.filter(_._2 == betCounts.head._2).map({ row => Bet(row._1, 0, year, 0.0, row._2, "", new DateTime())})
      }
      val highBets = {
        if (betCounts.isEmpty) List()
        else betCounts.filter(_._2 == betCounts.last._2).map({ row => Bet(row._1, 0, year, 0.0, row._2, "", new DateTime())})
      }
      sender ! BetProfiles(betCounts.map({ row => (row._2, row._3)}), highBets, lowBets)
    case YearsRequest =>

      val query = sql"""select distinct year as year from brackets order by year desc""".as[Int]
      val years = db.withSession{ implicit session =>
        query.list
      }
      val thisYear = new DateTime
      //sender ! thisYear.getYear :: years
      sender ! Years(List(2019, 2018, 2016))
    case CompetitionRequest(year, bet) =>
      val bets = db.withSession { implicit session =>
        val opposingBookId = bracketsTable.filter(x => {
          x.bookId === bet && x.year === year
        }).list.head.opposingBookId
        val gameResults = resultsTable.filter(x => {
          x.year === year && (x.bookId === bet || x.bookId === opposingBookId)
        }).sortBy(_.resultTimeStamp).list.map({ x => (x.bookId, x) }).toMap
        betsTable.filter({ x => (x.bookId === bet || x.bookId === opposingBookId) && x.year === year }).list.map({ bet =>
          if (gameResults.contains(bet.bookId)) betResult(bet, Some(gameResults(bet.bookId)))
          else betResult(bet, None)
        })
      }
      sender ! Competitors(bets)
  }
}
