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



import akka.actor.{ActorSystem, _}
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.{Directive, Directives}
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import org.bustos.whowon.WhoWonTables._
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.duration._
import scala.util.Properties.envOrElse

trait WhoWonRoutes extends Directives with WhoWonJsonProtocol {

  val logger = LoggerFactory.getLogger(getClass)
  implicit def system: ActorSystem
  implicit val timeout = Timeout(5 seconds)

  def whoWonData: ActorRef

  val routes = testRoute ~
    postBet ~
    getBets ~
    competition ~
    postGameResult ~
    betEntry ~
    bookIds ~
    gamesRequest ~
    missingGamesRequest ~
    winnings ~
    saveTicket ~
    betProfiles ~
    years ~
    logout ~
    login ~
    get {getFromResourceDirectory("webapp")}

  val keyLifespanMillis = 120000 * 1000 // 2000 minutes
  val expiration = DateTime.now + keyLifespanMillis
  val SessionKey = "WHOWON_SESSION"
  val UserKey = "WHOWON_USER"
  val ResponseTextHeader = "{\"responseText\": "

  def authentications = {
    val userPWD = envOrElse("WHOWON_USER_PASSWORDS", "mauricio,2015")
    userPWD.split(";").map(_.split(",")).map({ x => (x(0), x(1)) }).toMap
  }

  var sessionIds = Map.empty[String, String]

  def removeSession(email: String) = {
    sessionIds -= email
  }

  def authenticateSessionAdminUser(userName: String, sessionId: String): Directive[(String, String)] = Directive[(String, String)] { inner => ctx =>
    if (sessionIds.contains(userName) && userName == "mauricio") {
      if (sessionIds(userName) == sessionId) {
        inner((userName, sessionId))(ctx)
      } else ctx.reject()
    } else ctx.reject()
  }

  def authenticateSessionIdUser(userName: String, sessionId: String): Directive[(String, String)] = Directive[(String, String)] { inner => ctx =>
    if (sessionIds.contains(userName)) {
      if (sessionIds(userName) == sessionId) {
        inner((userName, sessionId))(ctx)
      } else ctx.reject()
    } else ctx.reject()
  }

  def authenticateUser(userName: String, password: String): Directive[(String, String)] = Directive[(String, String)] { inner => ctx =>
    if (authentications.contains(userName)) {
      if (authentications(userName) == password) {
        val sessionId = java.util.UUID.randomUUID.toString
        sessionIds += (userName -> sessionId)
        inner((userName, sessionId))(ctx)
      } else ctx.reject()
    }
    else ctx.reject()
  }

  def testRoute =
    path("test") { ctx =>
      ctx.complete(ResponseTextHeader + " \"Server is OK\"}")
    }

  def postBet = post {
    path("bets") {
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          entity(as[Bet]) { bet => {
            val future = whoWonData ? bet
            onSuccess(future) {
              case BetSubmitted => complete(200, ResponseTextHeader + "\"Bet Submitted\"}")
              case BetReplaced => complete(200, ResponseTextHeader + "\"Bet Replaced\"}")
              case UnknownPlayer => complete(400, ResponseTextHeader + "\"Unknown Player\"}")
              case UnknownBookId => complete(400, ResponseTextHeader + "\"Unknown Book Id\"}")
            }
          }
        }}
        }
      }}
    }
  }

  def competition = get {
    pathPrefix("competition" / IntNumber / IntNumber) { (year, betID) =>
      val future = whoWonData ? CompetitionRequest(year, betID)
      onSuccess(future) {
        case Competitors(list) => {
          complete { list }
        }
      }
    }
  }

  def getBets = get {
    pathPrefix("bets" / """.*""".r / IntNumber) { (userName, year) =>
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          authenticateSessionIdUser(username.value, sessionId.value) {
            (x, y) => {
              val future = whoWonData ? BetsRequest(userName, year)
              onSuccess(future) {
                case Bets(list) => complete { list }
                case UnknownPlayer => complete(400, ResponseTextHeader + "\"Unknown Player\"}")
    }}}}}}}}}

  def saveTicket = post {
    path("ticket") {
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          entity(as[ByteString]) { imageValue =>
            authenticateSessionIdUser(username.value, sessionId.value) {
              (x, y) => {
                val future = whoWonData ? TicketImage(username.value, imageValue)
                onSuccess(future) {
                  case list: List[Bet] => complete { list }
                  case location: String => complete(location)
                  case _ => complete(400, ResponseTextHeader + "\"Error processing image\"}")
                }
              }
            }
          }
        }
        }
      }
      } ~ getFromResource("webapp/login.html")
    }
  }

  def postGameResult = post {
    path("games" / IntNumber) { (year) =>
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          entity(as[GameResult]) { newResult =>
          val future = whoWonData ? newResult
          onSuccess(future) {
            case ResultSubmitted => complete(ResponseTextHeader + "\"Submitted\"}")
            case _ => complete(500, ResponseTextHeader + "\"Problem Submitting\"}")
          }
        }
        }}
      }}
  }}

  def gamesRequest = get {
    path("games" / IntNumber) { (year) =>
      val future = whoWonData ? GameResultsRequest(year)
      onSuccess(future) {
        case GameResults(list) => {
          complete { list }
        }
      }
    }
  }

  def missingGamesRequest = get {
    path("games" / IntNumber / "missing") { (year) =>
      val future = whoWonData ? MissingGameResultsRequest(year)
      onSuccess(future) {
        case BookIdsResults(list) => {
          complete { list }
        }
      }
    }
  }

  def bookIds = get {
    path("bookIds" / IntNumber) { (year) =>
      val future = whoWonData ? BookIdsRequest(year)
      onSuccess(future) {
        case BookIdsResults(list) => {
          complete { list }
        }
      }
    }
  }

  def betProfiles = get {
    path("betProfiles" /  IntNumber) { (year) =>
      val future = whoWonData ? BetProfilesRequest(year)
      onSuccess(future) {
        case x: BetProfiles => {
          complete { x.toJson }
        }
      }
    }
  }

  def winnings = get {
    path("winnings" / IntNumber) { (year) =>
      val future = whoWonData ? WinningsTrackRequest(year)
      onSuccess(future) {
        case WinningsTrack(timestamps, list) => {
          complete { WinningsTrack(timestamps, list) }
        }
      }
    }
  }

  def betEntry = get {
    path("") {
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          authenticateSessionIdUser(username.value, sessionId.value) {
            (x, y) => getFromResource("webapp/main.html")
          }
        }}
      }} ~ getFromResource("webapp/login.html")
    }
  }

  def years = get {
    path("years") {
      val future = whoWonData ? YearsRequest
      onSuccess(future) {
        case x: Years => {
          complete { x.years }
        }
        case _ => complete(400, ResponseTextHeader + "\"Problem Submitting\"}")
      }
    }
  }

  def logout = post {
    path("logout") {
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          setCookie(HttpCookie(SessionKey, value = "", expires = Some(expiration))) {
            removeSession(username.value)
            complete("/login")
          }}}}}}}

  def login = post {
      path("login") {
        formFields('inputName, 'inputPassword) { (inputName, inputPassword) =>
          authenticateUser(inputName, inputPassword) {
            (x, y) => {
              setCookie(HttpCookie(SessionKey, value = y, expires = Some(expiration))) {
                setCookie(HttpCookie(UserKey, value = x, expires = Some(expiration))) {
                  val future = whoWonData ? PlayerIdRequest(inputName)
                  onSuccess(future) {
                    case x: Player => complete("")
                    case x: UnknownPlayer => complete(400, "Unknown Player")
                  }
                }
              }
            }
          } ~ complete(400, "Bad player or password")
        } ~ complete(400, "Name and password not supplied")
      }
    }

}
