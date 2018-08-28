package com.pagerduty.akka.http.headerauthentication

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.requestauthentication.RequestAuthenticator
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationConfig
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationData.AuthFailedReason
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.metrics.NullMetrics
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.util.Try

class HeaderAuthenticatorSpec
    extends FreeSpecLike
    with Matchers
    with MockFactory {
  val authHeader = RawHeader("auth-header", "test")

  class TestAuthConfig extends HeaderAuthConfig {
    type Cred = String
    type AuthData = String
    type Permission = String

    def extractCredentials(request: HttpRequest)(
        implicit reqMeta: RequestMetadata): List[Cred] = ???

    def authenticate(credential: Cred)(
        implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = ???

    def authDataGrantsPermission(
        authData: AuthData,
        request: HttpRequest,
        permission: Option[Permission]
    )(implicit reqMeta: RequestMetadata): Option[AuthFailedReason] = ???

    def dataToAuthHeader(data: AuthData)(
        implicit reqMeta: RequestMetadata): HttpHeader = authHeader
    def authHeaderName: String = authHeader.name
  }

  implicit val reqMeta = RequestMetadata(None)
  val ac = new TestAuthConfig

  def buildHeaderAuthenticator(
      optAuthData: Option[String]): HeaderAuthenticator = {
    new HeaderAuthenticator {
      override def requestAuthenticator: RequestAuthenticator =
        new RequestAuthenticator {
          implicit def executionContext: ExecutionContextExecutor = ???
          def metrics = NullMetrics

          override def authenticate(
              authConfig: AuthenticationConfig
          )(request: HttpRequest,
            stripAuthorizationHeader: Boolean,
            requiredPermission: Option[authConfig.Permission])(
              handler: (HttpRequest,
                        Option[authConfig.AuthData]) => Future[HttpResponse])(
              implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
            handler(request,
                    optAuthData.asInstanceOf[Option[authConfig.AuthData]])
          }
        }
    }
  }

  def authHeader(req: HttpRequest): Option[HttpHeader] = {
    req.headers.find(h => h.is(authHeader.name))
  }

  "HeaderAuthentication" - {
    "adds auth header when authentication succeeds" in {
      val data = Some("auth-data")
      val headerAuth = buildHeaderAuthenticator(data)
      val request = HttpRequest()

      val handler = (req: HttpRequest, optAuthData: Option[String]) => {
        authHeader(req) should equal(Some(authHeader))
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(headerAuth.addAuthHeader(ac)(request)(handler), 1.seconds)
      response.status should be(OK)
    }

    "does not add auth header when authentication fails" in {
      val headerAuth = buildHeaderAuthenticator(None)
      val request = HttpRequest()
      val handler = (req: HttpRequest, optAuthData: Option[String]) => {
        authHeader(req) should equal(None)
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(headerAuth.addAuthHeader(ac)(request)(handler), 1.seconds)
      response.status should be(OK)
    }

    "returns 401 without calling handler if authentication is required" in {
      val headerAuth = buildHeaderAuthenticator(None)
      val request = HttpRequest()

      val handler = (req: HttpRequest, authData: String) => {
        throw new RuntimeException("Handler should not be called")
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(headerAuth.addAndRequireAuthHeader(ac)(request)(handler),
                     1.seconds)
    }

    "adds auth header and calls handler when auth is required and successful" in {
      val data = Some("auth-data")

      val headerAuth = buildHeaderAuthenticator(data)
      val request = HttpRequest()
      val handler = (req: HttpRequest, authData: String) => {
        authData should equal(data.get)
        authHeader(req) should equal(Some(authHeader))
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(headerAuth.addAndRequireAuthHeader(ac)(request)(handler),
                     1.seconds)
      response.status should be(OK)
    }
  }

}
