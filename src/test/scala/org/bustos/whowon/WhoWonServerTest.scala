/*

    Copyright (C) 2019 Mauricio Bustos (m@bustos.org)

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
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import akka.util.ByteString
import org.bustos.whowon.WhoWonData._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class WhoWonServerTest extends WordSpec with Matchers with ScalatestRouteTest with WhoWonRoutes {

  def actorRefFactory = system
  def context = actorRefFactory
  val whoWonData = system.actorOf(Props[WhoWonData], "whoWonData")
  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(5 seconds)

  "The service should " should {

    "be running on a dev database" in {
      WhoWonData.devDb shouldEqual true
      if (WhoWonData.devDb) {
        initializeData
        importResults(2019)
        importBrackets(2019)
      } else {
        System.exit(1)
      }
    }

    val testYear = "2019"
    val sessionPattern = """WHOWON_SESSION=(.*);.*""".r
    val userPattern = """WHOWON_USER=(.*);.*""".r

    var sessionId = ""
    var userId = ""

    "acknowledge server is up and running" in {
      Get("/test") ~> addHeader("inputName", "mb") ~> routes ~> check {
        responseAs[String] should include("Server is OK")
      }
    }

    "not be able to find a user" in {
      Post("/login", FormData(("inputName", "xxxx"), ("inputPassword", "xxxx"))) ~>
        routes ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "not allow a bad password" in {
      Post("/login", FormData(("inputName", "mauricio"), ("inputPassword", "xxxx"))) ~>
        routes ~> check {
        status shouldEqual StatusCodes.BadRequest
      }
    }

    "be able to login a user" in {
      Post("/login", FormData(("inputName", "mauricio"), ("inputPassword", "2015"))) ~>
        routes ~> check {
        response.getHeaders.forEach(x =>
          x.name match {
            case "Set-Cookie" => {
              x.value match {
                case sessionPattern(c) => sessionId = c
                case userPattern(c) => userId = c
              }
            }
          })
        status shouldEqual StatusCodes.OK
      }
    }

    "get available years" in {
      Get("/years") ~> routes ~> check {
        responseAs[String] should include ("2019")
      }
    }

    "get main display" in {
      Get("/") ~>
        addHeader("Cookie", "WHOWON_SESSION=" + sessionId) ~>
        addHeader("Cookie", "WHOWON_USER=" + userId) ~>
        routes ~> check {
        val r = responseAs[String]

        responseAs[String] should not include ("loginForm")
      }
    }

    "get login display" in {
      Get("/") ~>
        addHeader("Cookie", "WHOWON_SESSION=XXX") ~>
        addHeader("Cookie", "WHOWON_USER=" + userId) ~>
        routes ~> check {
        val r = responseAs[String]
        responseAs[String] should include ("loginForm")
      }
    }

    "get winnings" in {
      Get("/winnings/" + testYear) ~>
        routes ~> check {
        responseAs[String] should include ("timestamps")
      }
    }

    "get profiles" in {
      Get("/betProfiles/" + testYear) ~>
        routes ~> check {
        responseAs[String] should include ("largest")
      }
    }

    "get bookIds" in {
      Get("/bookIds/" + testYear) ~>
        routes ~> check {
        responseAs[String] should include ("seed")
      }
    }

    "get missingGames" in {
      Get("/games/" + testYear + "/missing") ~>
        routes ~> check {
        responseAs[String] should include ("seed")
      }
    }

    def postObject(uri: String, jsonRequest: ByteString): Unit = {
      val postRequest = HttpRequest(HttpMethods.POST, uri = uri, entity = HttpEntity(MediaTypes.`application/json`, jsonRequest))

      postRequest ~>
        addHeader("Cookie", "WHOWON_SESSION=XXX") ~>
        addHeader("Cookie", "WHOWON_USER=" + userId) ~> routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

    "post game result" in {
      postObject("/games/2019",
        ByteString("""{"year": 2019, "bookId": 717, "finalScore": 100, "firstHalfScore": 10, "opposingBookId": 718, "opposingFinalScore": 110, "opposingFirstHalfScore": 20, "firstTo15": true, "resultTimeStamp": "2019-03-16T01:00:00.000Z"}""".stripMargin))
      postObject("/games/2019",
        ByteString("""{"year": 2019, "bookId": 719, "finalScore": 100, "firstHalfScore": 10, "opposingBookId": 720, "opposingFinalScore": 110, "opposingFirstHalfScore": 20, "firstTo15": true, "resultTimeStamp": "2019-03-16T03:00:00.000Z"}""".stripMargin))
      postObject("/games/2019",
        ByteString("""{"year": 2019, "bookId": 721, "finalScore": 100, "firstHalfScore": 10, "opposingBookId": 722, "opposingFinalScore": 110, "opposingFirstHalfScore": 20, "firstTo15": true, "resultTimeStamp": "2019-03-16T04:00:00.000Z"}""".stripMargin))
      postObject("/games/2019",
        ByteString("""{"year": 2019, "bookId": 723, "finalScore": 100, "firstHalfScore": 10, "opposingBookId": 724, "opposingFinalScore": 110, "opposingFirstHalfScore": 20, "firstTo15": true, "resultTimeStamp": "2019-03-16T07:00:00.000Z"}""".stripMargin))
    }

    "get games" in {
      Get("/games/" + testYear) ~>
        routes ~> check {
        val r = responseAs[String]
        responseAs[String] should include ("undScore")
      }
    }

    "submit straight bet" in {
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 717, "year": """ + testYear + """, "spread_ml": 3.5, "amount": 1, "betType": "ST", "timestamp": "2019-03-14T04:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 719, "year": """ + testYear + """, "spread_ml": 3.5, "amount": 1, "betType": "ST", "timestamp": "2019-03-14T04:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 721, "year": """ + testYear + """, "spread_ml": 3.5, "amount": 1, "betType": "ST", "timestamp": "2019-03-14T04:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 723, "year": """ + testYear + """, "spread_ml": 3.5, "amount": 1, "betType": "ST", "timestamp": "2019-03-14T04:00:00.000Z"}""".stripMargin))
    }

    "submit straight over/under bet" in {
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 717, "year": """ + testYear + """, "spread_ml": 50, "amount": 1, "betType": "ST-OV", "timestamp": "2019-03-14T06:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 719, "year": """ + testYear + """, "spread_ml": 50, "amount": 1, "betType": "ST-OV", "timestamp": "2019-03-14T06:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 721, "year": """ + testYear + """, "spread_ml": 50, "amount": 1, "betType": "ST-OV", "timestamp": "2019-03-14T06:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 723, "year": """ + testYear + """, "spread_ml": 50, "amount": 1, "betType": "ST-OV", "timestamp": "2019-03-14T06:00:00.000Z"}""".stripMargin))
    }

    "submit moneyline bet" in {
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 717, "year": """ + testYear + """, "spread_ml": 110, "amount": 1, "betType": "ML", "timestamp": "2019-03-14T07:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 719, "year": """ + testYear + """, "spread_ml": 110, "amount": 1, "betType": "ML", "timestamp": "2019-03-14T07:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 721, "year": """ + testYear + """, "spread_ml": 110, "amount": 1, "betType": "ML", "timestamp": "2019-03-14T07:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 723, "year": """ + testYear + """, "spread_ml": 110, "amount": 1, "betType": "ML", "timestamp": "2019-03-14T07:00:00.000Z"}""".stripMargin))
    }

    "submit 1st half bet" in {
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 717, "year": """ + testYear + """, "spread_ml": 110, "amount": 1, "betType": "ML-1H", "timestamp": "2019-03-14T09:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 719, "year": """ + testYear + """, "spread_ml": 110, "amount": 1, "betType": "ML-1H", "timestamp": "2019-03-14T09:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 721, "year": """ + testYear + """, "spread_ml": 110, "amount": 1, "betType": "ML-1H", "timestamp": "2019-03-14T09:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 723, "year": """ + testYear + """, "spread_ml": 110, "amount": 1, "betType": "ML-1H", "timestamp": "2019-03-14T09:00:00.000Z"}""".stripMargin))
    }

    "submit 1st to 15 bet" in {
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 717, "year": """ + testYear  + """, "spread_ml": 110, "amount": 1, "betType": "ML-15", "timestamp": "2019-03-14T11:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 719, "year": """ + testYear  + """, "spread_ml": 110, "amount": 1, "betType": "ML-15", "timestamp": "2019-03-14T11:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 721, "year": """ + testYear  + """, "spread_ml": 110, "amount": 1, "betType": "ML-15", "timestamp": "2019-03-14T11:00:00.000Z"}""".stripMargin))
      postObject("/bets", ByteString("""{"userName": "mauricio", "bookId": 723, "year": """ + testYear  + """, "spread_ml": 110, "amount": 1, "betType": "ML-15", "timestamp": "2019-03-14T11:00:00.000Z"}""".stripMargin))
    }

    "be able to logout a user" in {
      Post("/logout") ~>
        addHeader("Cookie", "WHOWON_SESSION=" + sessionId) ~>
        addHeader("Cookie", "WHOWON_USER=" + userId) ~>
        routes ~> check {
        status shouldEqual StatusCodes.OK
      }
    }

  }

}
