package com.pagerduty.arrivals.impl.auth

import akka.http.scaladsl.model.StatusCodes.{Created, Unauthorized}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.{AuthFailedReason, AuthenticationConfig}
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Try

class RequireAuthenticationSpec extends FreeSpecLike with Matchers {

  class TestAuthConfig extends AuthenticationConfig {
    type Cred = String
    type AuthData = String
    type Permission = String

    def extractCredentials(request: HttpRequest)(implicit reqMeta: RequestMetadata): List[Cred] = ???

    def authenticate(credential: Cred)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = ???

    def authDataGrantsPermission(
        authData: AuthData,
        request: HttpRequest,
        permission: Option[Permission]
      )(implicit reqMeta: RequestMetadata
      ): Option[AuthFailedReason] = ???
  }

  "RequireAuthentication" - {
    val ac = new TestAuthConfig
    val unauthRequest = HttpRequest(uri = "/auth-failure")
    val authData = Some("auth-data")

    val ra = new RequestAuthenticator {
      override def authenticate(
          authConfig: AuthenticationConfig
        )(request: HttpRequest,
          requiredPermission: Option[authConfig.Permission]
        )(handler: (HttpRequest, Option[authConfig.AuthData]) => Future[HttpResponse]
        )(implicit reqMeta: RequestMetadata
        ): Future[HttpResponse] = {
        val data =
          if (request == unauthRequest) None
          else authData.asInstanceOf[Option[authConfig.AuthData]]
        handler(request, data)
      }

      def authServiceClient = ???
      implicit def executionContext = ???
      def metrics = ???
    }

    val requireAuthentication = new RequireAuthentication {
      val requestAuthenticator = ra
    }

    implicit val reqMeta = RequestMetadata(None)

    "responds with Unauthorized when authentication fails" in {
      val request = unauthRequest
      val handler = (request: HttpRequest, authData: String) => Future.successful(HttpResponse())

      val response =
        Await.result(requireAuthentication(ac)(request, None)(handler), 1.seconds)
      response.status should be(Unauthorized)
    }

    "calls the provided handler when authentication succeeds" in {
      val request = HttpRequest()
      val expectedResponse = HttpResponse(Created)
      val handler = (request: HttpRequest, authData: String) => Future.successful(expectedResponse)

      val response =
        Await.result(requireAuthentication(ac)(request, None)(handler), 1.seconds)
      response should equal(expectedResponse)
    }
  }
}
