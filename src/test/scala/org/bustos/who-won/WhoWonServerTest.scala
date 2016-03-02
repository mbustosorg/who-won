package org.bustos.whowon

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.routing.HttpService
import spray.http.StatusCodes._

class WhoWonServerTest extends Specification with Specs2RouteTest with HttpService {

  def actorRefFactory = system
  def context = actorRefFactory

  val routes = new WhoWonRoutes {
    def actorRefFactory = context
  }

  "The service" should {

    "Acknowledge server is up and running" in {
      Get("/test") ~> routes.routes ~> check {
        responseAs[String] must contain("Server is OK")
      }
    }

  }

}
