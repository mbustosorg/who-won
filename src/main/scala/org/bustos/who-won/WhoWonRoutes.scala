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

import javax.ws.rs.Path

import akka.actor._
import akka.pattern.ask
import com.gettyimages.spray.swagger.SwaggerHttpService
import com.wordnik.swagger.annotations._
import com.wordnik.swagger.model.ApiInfo
import org.bustos.whowon.WhoWonJsonProtocol._
import org.bustos.whowon.WhoWonTables._
import org.slf4j.LoggerFactory
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http.{DateTime, HttpCookie}
import spray.json._
import spray.routing._

import scala.reflect.runtime.universe._

class WhoWonServiceActor extends HttpServiceActor with ActorLogging {

  override def actorRefFactory = context

  val whoWonRoutes = new WhoWonRoutes {
    def actorRefFactory = context
  }

  def receive = runRoute(
    swaggerService.routes ~
    whoWonRoutes.routes ~
      get {getFromResourceDirectory("webapp")} ~
      get {getFromResource("webapp/index.html")})

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[WhoWonRoutes])
    override def apiVersion = "1.0"
    override def baseUrl = "/"
    override def docsPath = "api-docs"
    override def actorRefFactory = context
    override def apiInfo = Some(new ApiInfo("WhoWon Bet Trackig API",
      "API for interacting with the WhoWon Server.", "", "", "", ""))
  }
}

@Api(value = "/", description = "Primary Interface", produces = "application/json")
trait WhoWonRoutes extends HttpService with UserAuthentication {

  import java.net.InetAddress

  import UserAuthentication._

  val logger = LoggerFactory.getLogger(getClass)
  val system = ActorSystem("whoWonSystem")

  import system.dispatcher

  val whoWonData = system.actorOf(Props[WhoWonData], "whoWonData")

  val routes = testRoute ~
    postBet ~
    bets ~
    postGameResult ~
    betEntry ~
    bookIds ~
    gamesRequest ~
    missingGamesRequest ~
    winnings ~
    saveTicket ~
    login

  val authenticationRejection = RejectionHandler {
    case AuthenticationRejection(message) :: _ => complete(400, message)
  }

  val authorizationRejection = RejectionHandler {
    case AuthenticationRejection(message) :: _ => getFromResource("webapp/login.html")
  }

  val secureCookies: Boolean = {
    // Don't require HTTPS if running in development
    val hostname = InetAddress.getLocalHost.getHostName
    hostname != "localhost" && !hostname.contains("pro")
  }

  def redirectToHttps: Directive0 = {
    requestUri.flatMap { uri =>
      redirect(uri.copy(scheme = "https"), MovedPermanently)
    }
  }

  val isHttpsRequest: RequestContext => Boolean = { ctx =>
    (ctx.request.uri.scheme == "https" || ctx.request.headers.exists(h => h.is("x-forwarded-proto") && h.value == "https")) && secureCookies
  }

  def enforceHttps: Directive0 = {
    extract(isHttpsRequest).flatMap(
      if (_) pass
      else redirectToHttps
    )
  }

  val keyLifespanMillis = 120000 * 1000 // 2000 minutes
  val expiration = DateTime.now + keyLifespanMillis
  val SessionKey = "WHOWON_SESSION"
  val UserKey = "WHOWON_USER"

  @Path("test")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Operates connectivity test")
  @ApiImplicitParams(Array())
  @ApiResponses(Array())
  def testRoute =
    path("test") {
      respondWithMediaType(`application/json`) { ctx =>
        ctx.complete("{\"response\": \"Server is OK\"}")
      }
    }

  @Path("bets/{player}")
  @ApiOperation(httpMethod = "POST", response = classOf[String], value = "Post a bet for player")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "player", required = true, dataType = "integer", paramType = "path", value = "Player ID")
  ))
  @ApiResponses(Array())
  def postBet = post {
    path("bets") {
      respondWithMediaType(`application/json`) { ctx =>
        val newBet = ctx.request.entity.data.asString.parseJson.convertTo[Bet]
        val future = whoWonData ? newBet
        future onSuccess {
          case BetSubmitted => ctx.complete(200, "Bet Submitted")
          case BetReplaced => ctx.complete(200, "Bet Replaced")
          case UnknownPlayer => ctx.complete(400, "Unknown Player")
          case UnknownBookId => ctx.complete(400, "Unknown Book Id")
        }
      }
    }
  }

  @Path("bets/{player}")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Submitted bets for player")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "player", required = true, dataType = "integer", paramType = "path", value = "Player ID")
  ))
  @ApiResponses(Array())
  def bets = get {
    pathPrefix("bets" / """.*""".r / IntNumber) { (userName, year) =>
      respondWithMediaType(`application/json`) { ctx =>
        val future = whoWonData ? BetsRequest(userName, year)
        future onSuccess {
          case Bets(list) => ctx.complete(list.toJson.toString)
          case UnknownPlayer => ctx.complete(400, "Unknown Player")
        }
      }
    }
  }

  @Path("ticket")
  @ApiOperation(httpMethod = "POST", response = classOf[String], value = "Post a ticket image")
  @ApiImplicitParams(Array())
  @ApiResponses(Array())
  def saveTicket = post {
    path("ticket") {
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          handleRejections(authorizationRejection) {
            authenticate(authenticateSessionId(sessionId.content, username.content)) { authentication =>
              respondWithMediaType(`application/json`) { ctx =>
                val future = whoWonData ? TicketImage(username.content, ctx.request.entity.data.toByteString)
                future onSuccess {
                  case location: String => ctx.complete(location)
                  case _ => ctx.complete(400, "Error storing image")
                }
              }}
          }}
        }}
      }} ~ getFromResource("webapp/login.html")
    }

  @Path("games/{year}")
  @ApiOperation(httpMethod = "POST", response = classOf[String], value = "Post a game result")
  @ApiImplicitParams(Array())
  @ApiResponses(Array())
  def postGameResult = post {
    path("games" / IntNumber) { (year) =>
      respondWithMediaType(`application/json`) { ctx =>
        val newResult = ctx.request.entity.data.asString.parseJson.convertTo[GameResult]
        val future = whoWonData ? newResult
        future onSuccess {
          case ResultSubmitted => ctx.complete("{\"ResultText\": \"Submitted\"}")
          case _ => ctx.complete(500, "{``\"ResultText\": \"Problem Submitting\"}")
        }
      }
    }
  }

  @Path("games/{year}")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Get all game results for year")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "year", required = true, dataType = "integer", paramType = "path", value = "Year")
  ))
  @ApiResponses(Array())
  def gamesRequest = get {
    path("games" / IntNumber) { (year) =>
      respondWithMediaType(`application/json`) { ctx =>
        val future = whoWonData ? GameResultsRequest(year)
        future onSuccess {
          case GameResults(list) => {
            ctx.complete(list.toJson.toString)
          }
        }
      }
    }
  }

  @Path("games/{year}/missing")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Get games that don't yet have results")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "year", required = true, dataType = "integer", paramType = "path", value = "Year")
  ))
  @ApiResponses(Array())
  def missingGamesRequest = get {
    path("games" / IntNumber / "missing") { (year) =>
      respondWithMediaType(`application/json`) { ctx =>
        val future = whoWonData ? MissingGameResultsRequest(year)
        future onSuccess {
          case BookIdsResults(list) => {
            ctx.complete(list.toJson.toString)
          }
        }
      }
    }
  }

  @Path("bookIds/{year}")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Get all book ids for a year")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "year", required = true, dataType = "integer", paramType = "path", value = "Year")
  ))
  @ApiResponses(Array())
  def bookIds = get {
    path("bookIds" / IntNumber) { (year) =>
      respondWithMediaType(`application/json`) { ctx =>
        val future = whoWonData ? BookIdsRequest(year)
        future onSuccess {
          case BookIdsResults(list) => {
            ctx.complete(list.toJson.toString)
          }
        }
      }
    }
  }

  @Path("winnings/{year}")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Get summary winnings results for a year")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "year", required = true, dataType = "integer", paramType = "path", value = "Year")
  ))
  @ApiResponses(Array())
  def winnings = get {
    path("winnings" / IntNumber) { (year) =>
      respondWithMediaType(`application/json`) { ctx =>
        val future = whoWonData ? WinningsTrackRequest(year)
        future onSuccess {
          case WinningsTrack(timestamps, list) => {
            ctx.complete(WinningsTrack(timestamps, list).toJson.toString)
          }
        }
      }
    }
  }

  @Path("")
  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Bet entry")
  @ApiImplicitParams(Array())
  @ApiResponses(Array())
  def betEntry = get {
    path("") {
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          handleRejections(authorizationRejection) {
            authenticate(authenticateSessionId(sessionId.content, username.content)) { authentication =>
              getFromResource("webapp/main.html")
            }
          }
        }
        }
      }
      } ~ getFromResource("webapp/login.html")
    }
  }

  def admin = get {
    path("admin") {
      cookie("WHOWON_SESSION") { sessionId => {
        cookie("WHOWON_USER") { username => {
          handleRejections(authorizationRejection) {
            authenticate(authenticateSessionId(sessionId.content, username.content)) { authentication =>
              getFromResource("webapp/admin.html")
            }
          }
        }
        }
      }
      } ~ getFromResource("webapp/login.html")
    }
  }

  def login =
    post {
      path("login") {
        formFields('inputName, 'inputPassword) { (inputName, inputPassword) =>
          handleRejections(authenticationRejection) {
            authenticate(authenticateUser(inputName, inputPassword)) { authentication =>
              setCookie(HttpCookie(SessionKey, content = authentication.token, expires = Some(expiration))) {
                setCookie(HttpCookie(UserKey, content = inputName, expires = Some(expiration))) { ctx =>
                  val future = whoWonData ? PlayerIdRequest(inputName)
                  future onSuccess {
                    case x: Player => ctx.complete("")
                    case x: UnknownPlayer => ctx.complete(400, "Unknown Player")
                  }
                }
              }
            }
          }
        }
      }
    }

}
