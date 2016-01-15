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

    "Time series tests" in {
      Get("/v9999-12-31T00_00_00/byCompany/1/price") ~> routes.routes ~> check {
        responseAs[String] must contain(""""value":228""")
      }
      Get("/v9999-12-31T00_00_00/byCompany/1,2/price") ~> routes.routes ~> check {
        responseAs[String] must contain(""""value":225""")
      }
    }

    "Cross section tests" in {
      Get("/v9999-12-31T00_00_00/byMonth/600/price") ~> routes.routes ~> check {
        responseAs[String] must contain(""""value":18.125""")
      }
      Get("/v9999-12-31T00_00_00/byMonth/600/dehp,price") ~> routes.routes ~> check {
        responseAs[String] must contain(""""value":0.020817""")
      }
    }
  }

}
