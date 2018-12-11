package com.pagerduty.arrivals.impl.auth
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken, RawHeader}
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractRequest}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.{AuthFailedReason, AuthenticationConfig}
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.impl.headerauth.AuthHeaderDirectives
import com.pagerduty.metrics.NullMetrics
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class AuthenticationDirectivesSpec extends FreeSpec with Matchers with ScalatestRouteTest {
  implicit val metrics = NullMetrics
  implicit val reqMeta = RequestMetadata.fromRequest(HttpRequest())

  case class TestAuthData(userId: Int)
  val authData = TestAuthData(1)
  val badPermsAuthData = TestAuthData(2)

  class TestAuthConfig extends AuthenticationConfig {
    type Cred = OAuth2BearerToken
    type AuthData = TestAuthData
    type Permission = String

    def extractCredentials(request: HttpRequest)(implicit reqMeta: RequestMetadata): List[Cred] = {
      request.headers.flatMap {
        case Authorization(token: OAuth2BearerToken) => List(token)
        case _                                       => List()
      }.toList
    }

    def authenticate(credential: Cred)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = {
      credential.token match {
        case "GOODTOKEN" => Future.successful(Success(Some(authData)))
        case "BADPERMISSIONS" =>
          Future.successful(Success(Some(badPermsAuthData)))
        case "BADTOKEN" => Future.successful(Success(None))
        case "TRYFAILURE" =>
          Future.successful(Failure(new Exception("simulated exception")))
        case "FUTUREFAILURE" =>
          Future.failed(new Exception("simulated exception"))
      }
    }

    def authDataGrantsPermission(
        authData: AuthData,
        request: HttpRequest,
        permission: Option[Permission]
      )(implicit reqMeta: RequestMetadata
      ): Option[AuthFailedReason] = {
      authData match {
        case `badPermsAuthData` => Some(new AuthFailedReason("test"))
        case _                  => None
      }
    }
  }

  val testAuthConfig = new TestAuthConfig

  "authenticate directive" - {
    "extracts auth data when authentication succeeds" in {
      val route = AuthenticationDirectives.authenticate(testAuthConfig)(None)(reqMeta, metrics) { optAuthData =>
        complete {
          optAuthData should equal(Some(authData))
          HttpResponse()
        }
      }

      Get("/").withHeaders(Authorization(OAuth2BearerToken("GOODTOKEN"))) ~> route ~> check {
        response should equal(HttpResponse())
      }
    }

    "does not extract auth data when authentication fails because of no credentials" in {
      val route = AuthenticationDirectives.authenticate(testAuthConfig)(None)(reqMeta, metrics) { optAuthData =>
        complete {
          optAuthData should equal(None)
          HttpResponse()
        }
      }

      Get("/") ~> route ~> check {
        response should equal(HttpResponse())
      }
    }

    "does not extract auth data when authentication fails because of bad credentials" in {
      val route = AuthenticationDirectives.authenticate(testAuthConfig)(None)(reqMeta, metrics) { optAuthData =>
        complete {
          optAuthData should equal(None)
          HttpResponse()
        }
      }

      Get("/").withHeaders(Authorization(OAuth2BearerToken("BADTOKEN"))) ~> route ~> check {
        response should equal(HttpResponse())
      }
    }

    "does not extract auth data when authentication fails because of bad permissions" in {
      val route = AuthenticationDirectives.authenticate(testAuthConfig)(None)(reqMeta, metrics) { optAuthData =>
        complete {
          optAuthData should equal(None)
          HttpResponse()
        }
      }

      Get("/").withHeaders(Authorization(OAuth2BearerToken("BADPERMISSIONS"))) ~> route ~> check {
        response should equal(HttpResponse())
      }
    }

    "does not extract auth data when authentication fails because of try failure" in {
      val route = AuthenticationDirectives.authenticate(testAuthConfig)(None)(reqMeta, metrics) { optAuthData =>
        complete {
          optAuthData should equal(None)
          HttpResponse()
        }
      }

      Get("/").withHeaders(Authorization(OAuth2BearerToken("TRYFAILURE"))) ~> route ~> check {
        response should equal(HttpResponse())
      }
    }

    "does not extract auth data when authentication fails because of future failure" in {
      val route = AuthenticationDirectives.authenticate(testAuthConfig)(None)(reqMeta, metrics) { optAuthData =>
        complete {
          optAuthData should equal(None)
          HttpResponse()
        }
      }

      Get("/").withHeaders(Authorization(OAuth2BearerToken("FUTUREFAILURE"))) ~> route ~> check {
        response should equal(HttpResponse())
      }
    }
  }

  "requireAuthentication directive" - {
    "extracts auth data when authentication succeeds" in {
      val route = AuthenticationDirectives.requireAuthentication(testAuthConfig)(None)(reqMeta, metrics) { authData =>
        complete {
          authData should equal(authData)
          HttpResponse()
        }
      }

      Get("/").withHeaders(Authorization(OAuth2BearerToken("GOODTOKEN"))) ~> route ~> check {
        response should equal(HttpResponse())
      }
    }

    "returns 403 when authentication fails" in {
      val route = AuthenticationDirectives.requireAuthentication(testAuthConfig)(None)(reqMeta, metrics) { authData =>
        complete {
          authData should equal(authData)
          HttpResponse()
        }
      }

      Get("/").withHeaders(Authorization(OAuth2BearerToken("BADTOKEN"))) ~> route ~> check {
        response.status should equal(StatusCodes.Forbidden)
      }
    }
  }

}
