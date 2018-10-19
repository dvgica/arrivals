package com.pagerduty.akka.http.requestauthentication

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationConfig
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationData.AuthFailedReason
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.metrics.NullMetrics
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RequestAuthenticatorSpec
    extends FreeSpecLike
    with Matchers
    with MockFactory {
  implicit val reqMeta = RequestMetadata(None)

  case class TestAuthData(userId: Int)
  val authData = TestAuthData(1)
  val badPermsAuthData = TestAuthData(2)

  class TestAuthConfig extends AuthenticationConfig {
    type Cred = OAuth2BearerToken
    type AuthData = TestAuthData
    type Permission = String

    def extractCredentials(request: HttpRequest)(
        implicit reqMeta: RequestMetadata): List[Cred] = {
      request.headers.flatMap {
        case Authorization(token: OAuth2BearerToken) => List(token)
        case _ => List()
      }.toList
    }

    def authenticate(credential: Cred)(
        implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = {
      credential.token match {
        case "GOODTOKEN" => Future.successful(Success(Some(authData)))
        case "BADPERMISSIONS" =>
          Future.successful(Success(Some(badPermsAuthData)))
        case "BADTOKEN" => Future.successful(Success(None))
        case "PARSEFAILTOKEN" =>
          Future.successful(Failure(new Exception("simulated exception")))
        case "COMMSFAILTOKEN" =>
          Future.failed(new Exception("simulated exception"))
      }
    }

    def authDataGrantsPermission(
        authData: AuthData,
        request: HttpRequest,
        permission: Option[Permission]
    )(implicit reqMeta: RequestMetadata): Option[AuthFailedReason] = {
      authData match {
        case `badPermsAuthData` => Some(new AuthFailedReason("test"))
        case _ => None
      }
    }
  }

  "RequestAuthenticator" - {
    val ac = new TestAuthConfig

    val requestAuthenticator = new RequestAuthenticator {
      implicit def executionContext = ExecutionContext.global
      def metrics = NullMetrics
    }

    "calls handler with AuthenticationData when good token provided" in {
      val request =
        HttpRequest().addHeader(Authorization(OAuth2BearerToken("GOODTOKEN")))
      val handler = (req: HttpRequest, optAuthData: Option[TestAuthData]) => {
        optAuthData should equal(Some(authData))
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(
          requestAuthenticator
            .authenticate(ac)(request, requiredPermission = None)(
              handler
            ),
          1.seconds
        )
      response.status should be(OK)
    }

    "calls handler without auth data when good token provided but insufficient permissions" in {
      val request =
        HttpRequest().addHeader(
          Authorization(OAuth2BearerToken("BADPERMISSIONS")))
      val handler = (req: HttpRequest, optAuthData: Option[TestAuthData]) => {
        optAuthData should equal(None)
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(
          requestAuthenticator
            .authenticate(ac)(request, requiredPermission = None)(
              handler
            ),
          1.seconds
        )
      response.status should be(OK)
    }

    "calls handler without AuthenticationData when bad token provided" in {
      val request =
        HttpRequest().addHeader(Authorization(OAuth2BearerToken("BADTOKEN")))
      val handler = (req: HttpRequest, optAuthData: Option[TestAuthData]) => {
        optAuthData should equal(None)
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(
          requestAuthenticator.authenticate(ac)(request, None)(handler),
          1.seconds
        )
      response.status should be(OK)
    }

    "calls handler without AuthenticationData when no token provided" in {
      val request = HttpRequest()
      val handler = (req: HttpRequest, optAuthData: Option[TestAuthData]) => {
        optAuthData should equal(None)
        authorizationHeader(req) should equal(None)
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(
          requestAuthenticator.authenticate(ac)(request, None)(handler),
          1.seconds
        )
      response.status should be(OK)
    }

    "calls handler without AuthenticationData when auth service call fails because of unexpected response" in {
      val request =
        HttpRequest().addHeader(
          Authorization(OAuth2BearerToken("PARSEFAILTOKEN")))
      val handler = (req: HttpRequest, optAuthData: Option[TestAuthData]) => {
        optAuthData should equal(None)
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(
          requestAuthenticator.authenticate(ac)(request, None)(handler),
          1.seconds
        )
      response.status should be(OK)
    }

    "calls handler without AuthenticationData when auth service call fails totally" in {
      val request =
        HttpRequest().addHeader(
          Authorization(OAuth2BearerToken("COMMSFAILTOKEN")))
      val handler = (req: HttpRequest, optAuthData: Option[TestAuthData]) => {
        optAuthData should equal(None)
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(
          requestAuthenticator.authenticate(ac)(request, None)(handler),
          1.seconds
        )
      response.status should be(OK)
    }

    "calls handler without AuthenticationData when credentials are extracted" in {
      val request =
        HttpRequest()
          .addHeader(Authorization(OAuth2BearerToken("GOODTOKEN")))
          .addHeader(Authorization(OAuth2BearerToken("OTHERTOKEN")))
      val handler = (req: HttpRequest, optAuthData: Option[TestAuthData]) => {
        optAuthData should equal(None)
        Future.successful(HttpResponse())
      }

      val response =
        Await.result(
          requestAuthenticator.authenticate(ac)(request, None)(handler),
          1.seconds
        )
      response.status should be(OK)
    }
  }

  def authorizationHeader(r: HttpRequest): Option[HttpHeader] = {
    r.header[Authorization]
  }
}
