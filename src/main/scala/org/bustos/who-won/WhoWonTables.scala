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
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}

import scala.slick.driver.MySQLDriver.simple._
import spray.json._

object WhoWonTables {
  case class Bet(id: Int, playerId: Int, bookId: Int, spread: Float, amount: Float)
  case class Bracket(id: Int, year: Int, region: String, seed: Int, teamName: String)
  case class Player(id: Int, firstName: String, lastName: String, nickname: String)
  case class Result(year: Int,
                    homeTeamId: Int, homeTeamBookId: Int,
                    visitingTeamId: Int, visitingTeamBookId: Int,
                    homeScore: Int, visitingScore: Int,
                    resultTimeStamp: DateTime)

  val betsTable = TableQuery[BetsTable]
  val bracketsTable = TableQuery[BracketsTable]
  val playersTable = TableQuery[PlayersTable]
  val resultsTable = TableQuery[ResultsTable]

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

  implicit val bet = jsonFormat5(Bet)
  implicit val bracket = jsonFormat5(Bracket)
  implicit val player = jsonFormat4(Player)
  implicit val result = jsonFormat8(Result)
}

class BetsTable(tag: Tag) extends Table[WhoWonTables.Bet](tag, "bets") {
  import WhoWonTables.dateTime

  def id = column[Int]("id")
  def playerId = column[Int]("playerId")
  def bookdId = column[Int]("bookId")
  def spread = column[Float]("spread")
  def amount = column[Float]("amount")

  def * = (id, playerId, bookId, spread, amount) <> (WhoWonTables.Bet.tupled, WhoWonTables.Bet.unapply)
}

class BracketsTable(tag: Tag) extends Table[WhoWonTables.Bracket](tag, "brackets") {

  def id = column[Int]("id")
  def year = column[Int]("year")
  def region = column[String]("region")
  def seed = column[Int]("seed")
  def teamName = column[String]("teamName")

  def * = (id, year, region, seed, teamName) <> (WhoWonTables.Bracket.tupled, WhoWonTables.Bracket.unapply)
}

class PlayersTable(tag: Tag) extends Table[WhoWonTables.Player](tag, "players") {

  def id = column[Int]("id")
  def firstName = column[String]("firstName")
  def lastName = column[String]("lastName")
  def nickname = column[String]("nickname")

  def * = (id, firstName, lastName, nickname) <> (WhoWonTables.Player.tupled, WhoWonTables.Player.unapply)
}

class ResultsTable(tag: Tag) extends Table[WhoWonTables.Result](tag, "results") {
  import WhoWonTables.dateTime

  def year = column[Int]("year")
  def homeTeamId = column[Int]("homeTeamId")
  def homeTeamBookId = column[Int]("homeTeamBookId")
  def visitingTeamId = column[Int]("visitingTeamId")
  def visitingTeamBookId = column[Int]("visitingTeamBookId")
  def homeScore = column[Int]("homeScore")
  def visitingScore = column[Int]("visitingScore")
  def resultTimeStamp = column[DateTime]("resultTimeStamp")

  def * = (year, homeTeamId, homeTeamBookId, visitingTeamId, visitingTeamBookId, homeScore, visitingScore, resultTimeStamp) <> (WhoWonTables.Result.tupled, WhoWonTables.Result.unapply)
}