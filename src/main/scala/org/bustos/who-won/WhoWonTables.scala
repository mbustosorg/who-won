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

import org.joda.time._
import java.sql.Timestamp
import org.joda.time.format.{DateTimeFormat, ISODateTimeFormat, DateTimeFormatter}

import scala.slick.driver.MySQLDriver.simple._
import spray.json._

object WhoWonTables {

  // Constants
  val StraightBet = "ST"
  val MoneylineBet = "ML"
  val StraightBetPayoff = 1.0 - 0.0455
  // Base case classes
  case class Bet(userName: String, bookId: Int, year: Int, spread_ml: Float, amount: Float, betType: String)
  case class Bracket(bookId: Int, year: Int, region: String, seed: Int, teamName: String, gameTime: DateTime)
  case class Player(id: Int, userName: String, firstName: String, lastName: String, nickname: String)
  case class GameResult(bookId: Int, year: Int, score: Int, opposingScore: Int, resultTimeStamp: DateTime)
  // Utility case classes
  case class BetDisplay(bet: Bet, bracket: Bracket, payoff: Float, resultString: String)
  case class BetsRequest(playerId: String, year: Int)
  case class Bets(list: List[BetDisplay])
  case class GameResultsRequest(year: Int)
  case class GameResults(list: List[GameResult])
  case class PlayerIdRequest(userName: String)
  case class BookIdsRequest(year: Int)
  case class BookIdsResults(list: List[Bracket])
  // Error case classes
  case class UnknownPlayer()
  case class UnknownBookId()
  case class BetReplaced()
  case class BetSubmitted()

  val betsTable = TableQuery[BetsTable]
  val bracketsTable = TableQuery[BracketsTable]
  val playersTable = TableQuery[PlayersTable]
  val resultsTable = TableQuery[ResultsTable]

  val formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  implicit def dateTime =
    MappedColumnType.base[DateTime, Timestamp](
       dt => new Timestamp(dt.getMillis),
       ts => new DateTime(ts.getTime)
    )
}

object WhoWonJsonProtocol extends DefaultJsonProtocol {

  import WhoWonTables._

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    private val parserISO: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis();
    override def write(obj: DateTime) = JsString(parserISO.print(obj))
    override def read(json: JsValue) : DateTime = json match {
      case JsString(s) => parserISO.parseDateTime(s)
      case _ => throw new DeserializationException("Error info you want here ...")
    }
  }

  // Base case classes
  implicit val bet = jsonFormat6(Bet)
  implicit val bracket = jsonFormat6(Bracket)
  implicit val player = jsonFormat5(Player)
  implicit val result = jsonFormat5(GameResult)
  // Utility case classes
  implicit val betDisplay = jsonFormat4(BetDisplay)
  implicit val betsRequest = jsonFormat2(BetsRequest)
  implicit val bets = jsonFormat1(Bets)
  implicit val gameResultsRequest = jsonFormat1(GameResultsRequest)
  implicit val gameResults = jsonFormat1(GameResults)
  implicit val bookIdsRequest = jsonFormat1(BookIdsRequest)
  implicit val bookIdsResults = jsonFormat1(BookIdsResults)
}

class BetsTable(tag: Tag) extends Table[WhoWonTables.Bet](tag, "bets") {
  import WhoWonTables.dateTime

  def userName = column[String]("userName")
  def bookId = column[Int]("bookId")
  def year = column[Int]("year")
  def spread_ml = column[Float]("spread_ml")
  def amount = column[Float]("amount")
  def betType = column[String]("betType")

  def * = (userName, bookId, year, spread_ml, amount, betType) <> (WhoWonTables.Bet.tupled, WhoWonTables.Bet.unapply)
}

class BracketsTable(tag: Tag) extends Table[WhoWonTables.Bracket](tag, "brackets") {
  import WhoWonTables.dateTime

  def bookId = column[Int]("bookId")
  def year = column[Int]("year")
  def region = column[String]("region")
  def seed = column[Int]("seed")
  def teamName = column[String]("teamName")
  def gameTime = column[DateTime]("gameTime")

  def * = (bookId, year, region, seed, teamName, gameTime) <> (WhoWonTables.Bracket.tupled, WhoWonTables.Bracket.unapply)
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

  def bookId = column[Int]("bookId")
  def year = column[Int]("year")
  def score = column[Int]("score")
  def opposingScore = column[Int]("opposingScore")
  def resultTimeStamp = column[DateTime]("resultTimeStamp")

  def * = (bookId, year, score, opposingScore, resultTimeStamp) <> (WhoWonTables.GameResult.tupled, WhoWonTables.GameResult.unapply)
}