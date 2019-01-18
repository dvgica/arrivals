package com.pagerduty.arrivals.impl.headerauth

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthFailedReason
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import org.scalatest.{FreeSpec, Matchers}
import akka.http.scaladsl.server.Directives._

import scala.concurrent.Future
import scala.util.Try

class AuthHeaderDirectivesSpec extends FreeSpec with Matchers with ScalatestRouteTest {
  class TestAuthConfig extends HeaderAuthConfig {
    type AuthData = String
    type Permission = String

    def authenticate(request: HttpRequest)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] =
      ???

    def authDataGrantsPermission(
        authData: AuthData,
        request: HttpRequest,
        permission: Option[Permission]
      )(implicit reqMeta: RequestMetadata
      ): Option[AuthFailedReason] = ???

    def dataToAuthHeader(data: AuthData)(implicit reqMeta: RequestMetadata): HttpHeader =
      RawHeader(authHeaderName, "true")
    def authHeaderName: String = "X-Authed"
  }

  val reqMeta = RequestMetadata.fromRequest(HttpRequest())
  val testAuthConfig = new TestAuthConfig

  "addAuthHeader" - {
    "adds auth header if auth data is provided" in {

      val route = AuthHeaderDirectives.addAuthHeader(testAuthConfig)(Some("data"))(reqMeta) {
        extractRequest { reqWithHeader =>
          complete {
            reqWithHeader.headers.exists(_.is(testAuthConfig.authHeaderName.toLowerCase)) should be(true)
            HttpResponse()
          }
        }
      }

      Get("/") ~> route ~> check {
        response should equal(HttpResponse())
      }
    }

    "does not add auth header if no auth data is provided" in {
      val route = AuthHeaderDirectives.addAuthHeader(testAuthConfig)(None)(reqMeta) {
        extractRequest { reqWithHeader =>
          complete {
            reqWithHeader.headers.exists(_.is(testAuthConfig.authHeaderName.toLowerCase)) should be(false)
            HttpResponse()
          }
        }
      }

      Get("/") ~> route ~> check {
        response should equal(HttpResponse())
      }
    }
  }
}
