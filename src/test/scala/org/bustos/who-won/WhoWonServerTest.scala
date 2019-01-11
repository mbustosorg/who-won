package org.bustos.whowon

import akka.actor.Props
import akka.http.scaladsl.model.{FormData, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class WhoWonServerTest extends WordSpec with Matchers with ScalatestRouteTest with WhoWonRoutes {

  def actorRefFactory = system
  def context = actorRefFactory
  val whoWonData = system.actorOf(Props[WhoWonData], "whoWonData")

  "The service should " should {

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
      Get("/winnings/2017") ~>
        routes ~> check {
        responseAs[String] should include ("timestamps")
      }
    }

    "get profiles" in {
      Get("/betProfiles/2017") ~>
        routes ~> check {
        responseAs[String] should include ("largest")
      }
    }

    "get bookIds" in {
      Get("/bookIds/2017") ~>
        routes ~> check {
        responseAs[String] should include ("seed")
      }
    }

    "get missingGames" in {
      Get("/games/2017/missing") ~>
        routes ~> check {
        responseAs[String] should include ("seed")
      }
    }

    "get games" in {
      Get("/games/2016") ~>
        routes ~> check {
        val r = responseAs[String]
        responseAs[String] should include ("undScore")
      }
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
