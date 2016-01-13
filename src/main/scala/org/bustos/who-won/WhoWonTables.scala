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
  case class RestStopCounts()
  case class RiderUpdates()
  case class RiderRequest(bibNumber: Int)
  case class RiderEventsRequest(bibNumber: Int)
  case class RiderDelete(bibNumber: Int)
  case class RiderUpdateBib(bibNumber: Int, name: String)
  case class RiderUpdate(bibNumber: Int, latitude: Double, longitude: Double)
  case class RestStop(name: String, latitude: Double, longitude: Double)
  case class RiderConfirm(rider: Rider, update: RiderEvent)
  case class Rider(bibNumber: Int, name: String, registrationDate: DateTime)
  case class RiderEvent(bibNumber: Int, latitude: Double, longitude: Double, timestamp: DateTime)
  case class RiderSummary(bibNumber: Int, name: String, stop: String, timestamp: String, timestampObject: DateTime)
  case class RiderEventList(list: List[RiderSummary])

  val RaceStartHour = 9.0;
  val RestStops = List(
    RestStop("Start", 37.850787, -122.258015),
    RestStop("Moraga", 37.838825, -122.126016),
    RestStop("Briones", 37.925907, -122.162653),
    RestStop("Tilden", 37.904802, -122.244842),
    RestStop("End", 37.850787, -122.258015)
  )
  val RideOnRestStops = RestStops.slice(1, RestStops.length)
  val StartRestStop = RestStops(0)

  val RestStopsByName = RestStops.map({ x => (x.name, x)}).toMap

  val OffCourse = RestStop("Off Course", 0.0, 0.0)

  val riderTable = TableQuery[RiderTable]
  val riderEventTable = TableQuery[RiderEventTable]
  val latestEventPerRider = TableQuery[LatestEventPerRider]

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

  implicit val restStop = jsonFormat3(RestStop)
  implicit val riderUpdate = jsonFormat3(RiderUpdate)
  implicit val riderFormat = jsonFormat3(Rider)
  implicit val riderEventFormat = jsonFormat4(RiderEvent)
  implicit val riderConfirm = jsonFormat2(RiderConfirm)
  implicit val riderSummary = jsonFormat5(RiderSummary)

}

class RiderTable(tag: Tag) extends Table[WhoWonTables.Rider](tag, "rider") {
  import WhoWonTables.dateTime

  def bibNumber = column[Int]("bibNumber")
  def name = column[String]("name")
  def registrationDate = column[DateTime]("registrationDate")

  def * = (bibNumber, name, registrationDate) <> (WhoWonTables.Rider.tupled, WhoWonTables.Rider.unapply)
}

class RiderEventTable(tag: Tag) extends Table[WhoWonTables.RiderEvent](tag, "riderEvent") {
  import WhoWonTables.dateTime

  def bibNumber = column[Int]("bibNumber")
  def latitude = column[Double]("latitude")
  def longitude = column[Double]("longitude")
  def timestamp = column[DateTime]("timestamp")

  def * = (bibNumber, latitude, longitude, timestamp) <> (WhoWonTables.RiderEvent.tupled, WhoWonTables.RiderEvent.unapply)
}

class LatestEventPerRider(tag: Tag) extends Table[WhoWonTables.RiderEvent](tag, "latestEventPerRider") {
  import WhoWonTables.dateTime

  def bibNumber = column[Int]("bibNumber")
  def latitude = column[Double]("latitude")
  def longitude = column[Double]("longitude")
  def timestamp = column[DateTime]("timestamp")

  def * = (bibNumber, latitude, longitude, timestamp) <> (WhoWonTables.RiderEvent.tupled, WhoWonTables.RiderEvent.unapply)
}