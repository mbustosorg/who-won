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
import org.joda.time.{DateTimeZone, DateTime}
import spray.can.Http
import com.typesafe.config.ConfigFactory
import scala.util.Properties.envOrElse
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.github.tototoshi.csv._

object WhoWon extends App {

  def doMain = {

    implicit val system = ActorSystem()
    implicit val timeout = Timeout(DurationInt(5).seconds)

    val server = system.actorOf(Props[WhoWonServiceActor], "whowonRoutes")
    val config = ConfigFactory.load

    val port = envOrElse("PORT", config.getString("server.port"))

    if (args.length > 0) IO(Http) ? Http.Bind(server, "0.0.0.0", args(0).toInt)
    else IO(Http) ? Http.Bind(server, "0.0.0.0", port.toInt)
  }

  def updateRiders = {
    import WhoWonTables._
    import WhoWonData._
    import scala.slick.driver.MySQLDriver.simple._

    db.withSession { implicit session =>
      riderTable.filter(_.bibNumber > 0).delete
      riderEventTable.filter(_.bibNumber > 0).delete
      val reader = CSVReader.open(new File("/Users/mauricio/Downloads/Rider Tracking - Numeric.csv"))
      reader.foreach(fields => {
        println(fields)
        if (fields(1) != "" && fields(1).forall(_.isDigit)) {
          riderTable += Rider(fields(1).toInt, fields(0), new DateTime(DateTimeZone.UTC))
          val date = new DateTime(DateTimeZone.UTC)
          val localdate = new DateTime
          riderEventTable += RiderEvent(fields(1).toInt, RestStops(0).latitude, RestStops(0).longitude, date)
        }
      })
    }
  }

  //updateRiders
  doMain
}
