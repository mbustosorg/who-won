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

import akka.util.ByteString
import org.joda.time._
import java.sql.Timestamp
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat, DateTimeFormatter}

import scala.slick.driver.MySQLDriver.simple._
import spray.json._

import scala.util.Properties._

object WhoWonTables {

  // Constants
  val StraightBet = "ST"
  val MoneylineBet = "ML"
  val StraightBetPayoff = 1.0 - envOrElse("WHOWON_HOUSE_TAKE", "0.0455").toDouble
  // Base case classes
  case class Bet(userName: String, bookId: Int, year: Int, spread_ml: Double, amount: Double, betType: String, timestamp: DateTime)
  case class Bracket(bookId: Int, opposingBookId: Int, year: Int, region: String, seed: Int, teamName: String, gameTime: DateTime)
  case class Player(id: Int, userName: String, firstName: String, lastName: String, nickname: String)
  case class GameResult(year: Int, bookId: Int, score: Int, opposingBookId: Int, opposingScore: Int, resultTimeStamp: DateTime)
  case object YearsRequest
  // Utility case classes
  case class BetDisplay(bet: Bet, bracket: Bracket, payoff: Double, resultString: String)
  case class BetsRequest(playerId: String, year: Int)
  case class Bets(list: List[BetDisplay])
  case class GameResultsRequest(year: Int)
  case class MissingGameResultsRequest(year: Int)
  case class GameResultDisplay(favBookId: Int, undBookId: Int, favSeed: Int, undSeed: Int, favName: String, undName: String, favScore: Int, undScore: Int, timestamp: DateTime)
  case class GameResults(list: List[GameResultDisplay])
  case class PlayerIdRequest(userName: String)
  case class BookIdsRequest(year: Int)
  case class BookIdsResults(list: List[Bracket])
  case class WinningsTrackRequest(year: Int)
  case class PlayerWinnings(userName: String, winnings: List[Double], percentage: List[Int])
  case class WinningsTrack(timestamps: List[DateTime], list: List[PlayerWinnings])
  case class TicketImage(userName: String, image: ByteString)
  case class BetProfilesRequest(year: Int)
  case class BetProfiles(values: List[(Double, Int)], largest: List[Bet], smallest: List[Bet])
  case class CompetitionRequest(year: Int, bookId: Int)
  case class Competitors(others: List[BetDisplay])
  // Error case classes
  case class UnknownPlayer()
  case class UnknownBookId()
  // Results case classes
  case class BetReplaced()
  case class BetSubmitted()
  case class ResultSubmitted()

  val betsTable = TableQuery[BetsTable]
  val bracketsTable = TableQuery[BracketsTable]
  val playersTable = TableQuery[PlayersTable]
  val resultsTable = TableQuery[ResultsTable]

  val formatter = DateTimeFormat.forPattern("MM/dd/yyyy HH:mm:ss")

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
  implicit def dateTime =
    MappedColumnType.base[DateTime, Timestamp](
       dt => new Timestamp(dt.getMillis),
       ts => new DateTime(ts.getTime)
    )
}

object WhoWonJsonProtocol extends DefaultJsonProtocol {

  import WhoWonTables._

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    private val parserISO: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis()
    private val parserMillisISO: DateTimeFormatter = ISODateTimeFormat.dateTime()
    override def write(obj: DateTime) = JsString(parserISO.print(obj))
    override def read(json: JsValue) : DateTime = json match {
      case JsString(s) =>
        try {
          parserISO.parseDateTime(s)
        } catch {
          case _: Throwable => parserMillisISO.parseDateTime(s)
        }
      case _ => throw new DeserializationException("Error info you want here ...")
    }
  }

  // Base case classes
  implicit val bet = jsonFormat7(Bet)
  implicit val bracket = jsonFormat7(Bracket)
  implicit val player = jsonFormat5(Player)
  implicit val result = jsonFormat6(GameResult)
  // Utility case classes
  implicit val betDisplay = jsonFormat4(BetDisplay)
  implicit val betsRequest = jsonFormat2(BetsRequest)
  implicit val bets = jsonFormat1(Bets)
  implicit val gameResultsRequest = jsonFormat1(GameResultsRequest)
  implicit val gameResultDisplay = jsonFormat9(GameResultDisplay)
  implicit val gameResults = jsonFormat1(GameResults)
  implicit val bookIdsRequest = jsonFormat1(BookIdsRequest)
  implicit val bookIdsResults = jsonFormat1(BookIdsResults)
  implicit val playerWinnings = jsonFormat3(PlayerWinnings)
  implicit val winningsTrack = jsonFormat2(WinningsTrack)
  implicit val betProfilesRequest = jsonFormat3(BetProfiles)
  implicit val competitors = jsonFormat1(Competitors)
}

class BetsTable(tag: Tag) extends Table[WhoWonTables.Bet](tag, "bets") {
  import WhoWonTables.dateTime

  def userName = column[String]("userName")
  def bookId = column[Int]("bookId")
  def year = column[Int]("year")
  def spread_ml = column[Double]("spread_ml")
  def amount = column[Double]("amount")
  def betType = column[String]("betType")
  def timestamp = column[DateTime]("timestamp")

  def * = (userName, bookId, year, spread_ml, amount, betType, timestamp) <> (WhoWonTables.Bet.tupled, WhoWonTables.Bet.unapply)
}

class BracketsTable(tag: Tag) extends Table[WhoWonTables.Bracket](tag, "brackets") {
  import WhoWonTables.dateTime

  def bookId = column[Int]("bookId")
  def opposingBookId = column[Int]("opposingBookId")
  def year = column[Int]("year")
  def region = column[String]("region")
  def seed = column[Int]("seed")
  def teamName = column[String]("teamName")
  def gameTime = column[DateTime]("gameTime")

  def * = (bookId, opposingBookId, year, region, seed, teamName, gameTime) <> (WhoWonTables.Bracket.tupled, WhoWonTables.Bracket.unapply)
}

class PlayersTable(tag: Tag) extends Table[WhoWonTables.Player](tag, "players") {

  def id = column[Int]("id")
  def userName = column[String]("userName")
  def firstName = column[String]("firstName")
  def lastName = column[String]("lastName")
  def nickname = column[String]("nickname")

  def * = (id, userName, firstName, lastName, nickname) <> (WhoWonTables.Player.tupled, WhoWonTables.Player.unapply)
}

class ResultsTable(tag: Tag) extends Table[WhoWonTables.GameResult](tag, "results") {
  import WhoWonTables.dateTime

  def year = column[Int]("year")
  def bookId = column[Int]("bookId")
  def score = column[Int]("score")
  def opposingBookId = column[Int]("opposingBookId")
  def opposingScore = column[Int]("opposingScore")
  def resultTimeStamp = column[DateTime]("resultTimeStamp")

  def * = (year, bookId, score, opposingBookId, opposingScore, resultTimeStamp) <> (WhoWonTables.GameResult.tupled, WhoWonTables.GameResult.unapply)
}