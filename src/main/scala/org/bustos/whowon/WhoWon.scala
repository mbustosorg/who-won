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

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Properties.envOrElse

object WhoWon extends App with WhoWonRoutes {

  implicit val system: ActorSystem = ActorSystem("WhoWonAkkaHttpServer")
  implicit val materializer = ActorMaterializer()

  val whoWonData = system.actorOf(Props[WhoWonData], "whoWonData")
  //WhoWonData.initializeData
  //List(2021, 2019, 2018, 2016).map({ year =>
  List(2023).map({ year =>
    //WhoWonData.importBets(year)
    //WhoWonData.importBrackets(year)
    //WhoWonData.importResults(year)
  })
  lazy val serverRoutes: Route = routes

  val config = ConfigFactory.load
  val portFromEnv = envOrElse("PORT", "") != ""
  val port = envOrElse("PORT", config.getString("server.port"))

  if (args.length > 0) Http().bindAndHandle(serverRoutes, "0.0.0.0", args(0).toInt)
  else Http().bindAndHandle(serverRoutes, "0.0.0.0", port.toInt)

  Await.result(system.whenTerminated, Duration.Inf)

}
