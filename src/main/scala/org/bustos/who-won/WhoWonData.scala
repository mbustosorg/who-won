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
import org.bustos.whowon.WhoWonTables.{RiderEvent, Rider}
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

  var riders: Map[Int, Rider] = {
    val fullList = db.withSession { implicit session =>
      riderTable.list.map({ x => (x.bibNumber -> Rider(x.bibNumber, x.name, x.registrationDate))})
    }
    fullList.toMap
  }
  var ridersByName: Map[String, Rider] = {
    val fullList = db.withSession { implicit session =>
      riderTable.list.map({ x => (x.name -> Rider(x.bibNumber, x.name, x.registrationDate))})
    }
    fullList.toMap
  }
  var riderEvents = Map.empty[Int, RiderEvent]

  def atStop(event: RiderEvent, stop: RestStop): Boolean = {
    (stop.latitude - event.latitude).abs < quarterMile && (stop.longitude - event.longitude).abs < quarterMile
  }

  def restStop(event: RiderEvent, stops: List[RestStop]): RestStop = {
    stops.find({ atStop(event, _) }) match {
      case Some(stop) => stop
      case _ => OffCourse
    }
  }

  def stopName(event: RiderEvent, context: List[RiderEvent]): String = {
    if (restStop(event, RestStops).name == "Off Course") {
      val lat = event.latitude
      val lon = event.longitude
      "<a href=http://maps.google.com/maps?z=12&t=m&q=loc:" + f"$lat%1.4f" + "+" + f"$lon%1.4f>" + f"$lat%1.4f" + ", " + f"$lon%1.4f" +"</a>"
    } else {
      val sorted = context.sortWith({ (x, y) => x.timestamp.getMillis < y.timestamp.getMillis })
      if (event.timestamp.getHourOfDay < RaceStartHour + 2.0) restStop(event, RestStops).name
      else restStop(event, RideOnRestStops).name
    }
  }

  def riderCounts: List[(String, Int)] = {
    val stopCounts = lastRiderEventSummary.groupBy(_.stop).map({ case (x, y) => (x, y.length) })
    RestStops.map({ x => if (stopCounts.contains(x.name)) (x.name, stopCounts(x.name)) else (x.name, 0)})
  }

  def lastRiderEventSummary = {
    db.withSession { implicit session =>
      val events: Map[Int, List[RiderEvent]] = riderEventTable.sortBy(p => (p.bibNumber.asc, p.timestamp.desc)).list.groupBy(_.bibNumber)
      events.map({ case (bibNumber, curEvents) =>
        if (riders.contains(curEvents.head.bibNumber))
          RiderSummary(curEvents.head.bibNumber,
            riders(curEvents.head.bibNumber).name,
            stopName(curEvents.head, curEvents),
            hhmmssFormatter.print(curEvents.head.timestamp.toDateTime(DateTimeZone.forID("America/Los_Angeles"))),
            curEvents.head.timestamp
          )
        else RiderSummary(0, "", "", "", null)
      }).filter({ _.bibNumber > 0 }).toList.sortWith({ (x, y) => x.timestampObject.isBefore(y.timestampObject) })
    }
  }

  def receive = {
    case Rider(bibNumber, name, addTime) => {
      val rider = {
        if (riders.contains(bibNumber)) riders(bibNumber)
        else {
          val riders = db.withSession { implicit session =>
            riderTable.filter(_.bibNumber === bibNumber).list
          }
          if (riders.isEmpty) Rider(0, "", null)
          else riders.head
        }
      }
      if (rider.bibNumber != 0) sender ! Rider(0, "", null)
      else {
        val newRider = Rider(bibNumber, name, addTime)
        db.withSession { implicit session =>
          riderTable += newRider
          riderEventTable += RiderEvent(bibNumber, RestStopsByName("Start").latitude, RestStopsByName("Start").longitude, new DateTime(DateTimeZone.UTC))
        }
        riders += (bibNumber -> newRider)
        ridersByName += (name -> newRider)
        sender ! newRider
      }
    }
    case RiderRequest(bibNumber) => {
      val rider = {
        if (riders.contains(bibNumber)) riders(bibNumber)
        else {
          val riders = db.withSession { implicit session =>
            riderTable.filter(_.bibNumber === bibNumber).list
          }
          if (riders.isEmpty) Rider(0, "", null)
          else riders.head
        }
      }
      sender ! rider
    }
    case RiderDelete(bibNumber) => {
      val rider = {
        if (riders.contains(bibNumber)) riders(bibNumber)
        else {
          val riders = db.withSession { implicit session =>
            riderTable.filter(_.bibNumber === bibNumber).list
          }
          if (riders.isEmpty) Rider(0, "", null)
          else riders.head
        }
      }
      if (rider.bibNumber > 0) {
        db.withSession { implicit session =>
          riderTable.filter(_.bibNumber === bibNumber).delete
          riderEventTable.filter(_.bibNumber === rider.bibNumber).delete
        }
        riders -= bibNumber
        ridersByName -= rider.name
      }
      sender ! rider
    }
    case RiderUpdateBib(bibNumber, name) => {
      val rider = {
        if (ridersByName.contains(name)) ridersByName(name)
        else {
          val riders = db.withSession { implicit session =>
            riderTable.filter(_.name === name).list
          }
          if (riders.isEmpty) Rider(0, "", null)
          else riders.head
        }
      }
      if (rider.bibNumber > 0) {
        val updatedRider = Rider(bibNumber, name, new org.joda.time.DateTime(org.joda.time.DateTimeZone.UTC))
        db.withSession { implicit session =>
          riderTable.filter(_.bibNumber === rider.bibNumber).delete
          riderEventTable.filter(_.bibNumber === rider.bibNumber).delete
          riderTable += updatedRider
        }
        riders -= bibNumber
        ridersByName -= name
        riders += (bibNumber -> updatedRider)
        ridersByName += (name -> updatedRider)
        sender ! updatedRider
      } else sender ! rider
    }
    case RiderUpdate(bibNumber, latitude, longitude) =>
      val riderConfirm = {
        if (!riders.contains(bibNumber)) RiderConfirm(Rider(0, "", null), RiderEvent(0, 0.0, 0.0, new DateTime(DateTimeZone.UTC)))
        else {
          val event = RiderEvent(bibNumber, latitude, longitude, new DateTime(DateTimeZone.UTC))
          db.withSession { implicit session =>
            riderEventTable += event
          }
          logger.info(event.toString)
          RiderConfirm(riders(bibNumber), event)
        }
      }
      sender ! riderConfirm
    case RestStopCounts => sender ! riderCounts.toJson.toString
    case RiderUpdates => sender ! lastRiderEventSummary.toJson.toString
    case RiderEventsRequest(bibNumber) => {
      val eventList = db.withSession { implicit session =>
        riderEventTable.filter(_.bibNumber === bibNumber).sortBy(_.timestamp desc).list
      }
      sender ! RiderEventList(eventList.map ({ x =>
        RiderSummary(x.bibNumber,
          riders(x.bibNumber).name,
          stopName(x, eventList),
          hhmmssFormatter.print(x.timestamp.toDateTime(DateTimeZone.forID(WestCoastId))),
          x.timestamp
        )})
      )
    }
  }
}
