package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.{HttpProxy, LocalPortUpstream}
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationData.AuthFailedReason
import com.pagerduty.akka.http.support.RequestMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future
import scala.util.Try

class AuthProxyControllerSpec
    extends FreeSpecLike
    with Matchers
    with ScalatestRouteTest
    with MockFactory { outer =>
  implicit val reqMeta = RequestMetadata(None)

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
        implicit reqMeta: RequestMetadata): HttpHeader = ???
    def authHeaderName: String = ???
  }

  "AuthProxyController" - {
    val proxyStub = stub[HttpProxy]

    val authedRequest = HttpRequest(uri = "i-am-authed")

    val headerAuthStub = new HeaderAuthenticator {
      override def addAuthHeader(
          authConfig: HeaderAuthConfig
      )(request: HttpRequest,
        stripAuthorizationHeader: Boolean = true,
        requiredPermission: Option[authConfig.Permission] = None)(
          handler: HttpRequest => Future[HttpResponse])(
          implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
        handler(authedRequest)
      }

      override def requestAuthenticator = ???
    }

    val c = new AuthProxyController[TestAuthConfig] {
      val authConfig = new TestAuthConfig
      def httpProxy = proxyStub
      def localHostname = "localhost"
      def headerAuthenticator = headerAuthStub
    }
    val upstream = new LocalPortUpstream {
      val localPort = 1234
      val metricsTag = "test"
    }

    "authenticates and proxies requests" in {
      val expectedResponse = HttpResponse(201)

      (proxyStub.request _)
        .when(authedRequest, *)
        .returns(Future.successful(expectedResponse))

      Seq(Get(_: String),
          Post(_: String),
          Put(_: String),
          Delete(_: String),
          Patch(_: String)).foreach { verb =>
        verb("/") ~> c.proxyRoute(upstream, Some("permission")) ~> check {
          handled shouldBe true
          response should equal(expectedResponse)
        }
      }
    }

    "transforms, authenticates and proxies requests" in {
      val expectedResponse = HttpResponse(201)

      (proxyStub.request _)
        .when(authedRequest, *)
        .returns(Future.successful(expectedResponse))

      val transformer = (request: HttpRequest) => {
        val uriWithQueryParam =
          request.uri.withQuery(Query("filter_for_manual_run" -> "true"))
        request.withUri(uriWithQueryParam)
      }

      Seq(Get(_: String),
          Post(_: String),
          Put(_: String),
          Delete(_: String),
          Patch(_: String)).foreach { verb =>
        verb("/") ~> c.proxyRoute(
          upstream,
          requestTransformer = Some(transformer),
          requiredPermission = Some("permission")
        ) ~> check {
          handled shouldBe true
          response should equal(expectedResponse)
        }
      }
    }

    "authenticates and proxies prefixed requests" in {
      val expectedResponse = HttpResponse(204)

      (proxyStub.request _)
        .when(authedRequest, *)
        .returns(Future.successful(expectedResponse))

      Seq(
        "/api/v2/blabla",
        "/api/v2/incidents?foo=bar&baz=foo",
        "/api/v2/hello/foo/bar.ext",
        "/api/v2",
        "/api/v2/"
      ).foreach { url =>
        Seq(Get(_: String),
            Post(_: String),
            Put(_: String),
            Delete(_: String),
            Patch(_: String)).foreach { verb =>
          verb(url) ~> c.prefixProxyRoute(
            "api" / "v2",
            upstream,
            Some("permission")
          ) ~> check {
            handled shouldBe true
            response should equal(expectedResponse)
          }
        }
      }
    }
  }
}
