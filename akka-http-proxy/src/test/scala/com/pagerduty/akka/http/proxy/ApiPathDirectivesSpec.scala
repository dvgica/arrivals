package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.server.Directives._
import org.scalatest.{FreeSpecLike, Matchers}

class ApiPathDirectivesSpec
    extends FreeSpecLike
    with Matchers
    with ScalatestRouteTest {
  "ApiPathDirectives" - {
    val dir = new ApiPathDirectives {}

    "convert v1 paths to v2" in {
      val testPaths = Map(
        "/api/v1/response_plays" -> "/api/v2/response_plays",
        "/api/v1/response_plays/" -> "/api/v2/response_plays/",
        "/api/v2/response_plays" -> "/api/v2/response_plays",
        "/api/v1/response_plays/foo/bar/baz/api/v1/foo" -> "/api/v2/response_plays/foo/bar/baz/api/v1/foo",
        "/api/v1/response_plays/foo?bar=baz&blah=blarg" -> "/api/v2/response_plays/foo?bar=baz&blah=blarg"
      )

      val route = dir.mapApiV1ToV2Path {
        extractRequest { req =>
          complete(req.uri.toString)
        }
      }

      testPaths.foreach {
        case (v1Path, expectedV2Path) =>
          Get(v1Path) ~> route ~> check {
            responseAs[String] should equal(
              s"http://example.com$expectedV2Path")
          }
      }
    }

    "prepend v2 to paths" in {
      val testPaths = Map(
        "/response_plays" -> "/api/v2/response_plays",
        "/response_plays/" -> "/api/v2/response_plays/",
        "/response_plays/foo/bar/baz/api/v1/foo" -> "/api/v2/response_plays/foo/bar/baz/api/v1/foo",
        "/response_plays/foo?bar=baz&blah=blarg" -> "/api/v2/response_plays/foo?bar=baz&blah=blarg"
      )

      val route = dir.prependApiV2Path {
        extractRequest { req =>
          complete(req.uri.toString)
        }
      }

      testPaths.foreach {
        case (v1Path, expectedV2Path) =>
          Get(v1Path) ~> route ~> check {
            responseAs[String] should equal(
              s"http://example.com$expectedV2Path")
          }
      }
    }
  }
}
