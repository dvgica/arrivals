package com.pagerduty.arrivals.impl.authproxy

import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthFailedReason
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.impl.headerauth.HeaderAuthenticator
import com.pagerduty.arrivals.impl.proxy.HttpProxy
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AuthProxyControllerSpec extends FreeSpecLike with Matchers with ScalatestRouteTest with MockFactory { outer =>
  implicit val reqMeta = RequestMetadata(None)

  class TestAuthConfig extends HeaderAuthConfig {
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

    def dataToAuthHeader(data: AuthData)(implicit reqMeta: RequestMetadata): HttpHeader = ???
    def authHeaderName: String = ???
  }

  "AuthProxyController" - {
    val testAuthData = "auth-data"
    val expectedResponse = HttpResponse(201)
    val expectedTransformedResponse = HttpResponse(302)

    val proxyStub = new HttpProxy[String](null, null)(null, null, null) {
      override def apply(request: HttpRequest, upstream: Upstream[String], t: Any): Future[HttpResponse] =
        if (request.uri.toString.contains("transformed")) {
          Future.successful(expectedTransformedResponse)
        } else {
          Future.successful(expectedResponse)
        }
    }
    val authedRequest = HttpRequest(uri = "i-am-authed")

    val headerAuthStub = new HeaderAuthenticator {
      override def addAuthHeader(
          authConfig: HeaderAuthConfig
        )(request: HttpRequest,
          requiredPermission: Option[authConfig.Permission] = None
        )(handler: (HttpRequest, Option[authConfig.AuthData]) => Future[HttpResponse]
        )(implicit reqMeta: RequestMetadata
        ): Future[HttpResponse] = {
        handler(authedRequest, Some(testAuthData).asInstanceOf[Option[authConfig.AuthData]])
      }

      override def requestAuthenticator = ???
    }

    val c = new AuthProxyController[TestAuthConfig, String] {
      implicit val executionContext =
        scala.concurrent.ExecutionContext.Implicits.global
      val authConfig = new TestAuthConfig
      def httpProxy = proxyStub
      def authProxyRequestHandler =
        new AuthProxyRequestHandler[String, TestAuthConfig#AuthData] {
          val executionContext = ExecutionContext.global
        }
      def headerAuthenticator = headerAuthStub
    }
    val upstream = new Upstream[String] {
      def addressRequest(request: HttpRequest, addressingConfig: String): HttpRequest = request

      val metricsTag = "test"
    }

    "authenticates and proxies requests" in {
      Seq(Get(_: String), Post(_: String), Put(_: String), Delete(_: String), Patch(_: String)).foreach { verb =>
        verb("/") ~> c.authProxyRoute(upstream, Some("permission")) ~> check {
          handled shouldBe true
          response should equal(expectedResponse)
        }
      }
    }

//    "transforms, authenticates and proxies requests" in {
//      val requestTransformer = new RequestFilter[String] {
//        def apply(
//            request: HttpRequest,
//            optAuthData: Option[String]): Future[EithHttpRequest] = {
//          optAuthData should equal(Some(testAuthData))
//
//          Future.successful(request.withUri("transformed"))
//        }
//      }
//
//      val transformedResponse = HttpResponse(StatusCodes.MethodNotAllowed)
//
//      val responseTransformer = new AuthedResponseFilter[String] {
//        def apply(request: HttpRequest,
//                  response: HttpResponse,
//                  optAuthData: Option[String]): Future[HttpResponse] = {
//          optAuthData should equal(Some(testAuthData))
//          response should equal(expectedTransformedResponse)
//
//          Future.successful(transformedResponse)
//        }
//      }
//
//      Seq(Get(_: String),
//          Post(_: String),
//          Put(_: String),
//          Delete(_: String),
//          Patch(_: String)).foreach { verb =>
//        verb("/") ~> c.authProxyRoute(
//          upstream,
//          requestTransformer = Some(requestTransformer),
//          responseTransformer = Some(responseTransformer)
//        ) ~> check {
//          handled shouldBe true
//          response should equal(transformedResponse)
//        }
//      }
//    }

    "authenticates and proxies prefixed requests" in {
      Seq(
        "/api/v2/blabla",
        "/api/v2/incidents?foo=bar&baz=foo",
        "/api/v2/hello/foo/bar.ext",
        "/api/v2",
        "/api/v2/"
      ).foreach { url =>
        Seq(Get(_: String), Post(_: String), Put(_: String), Delete(_: String), Patch(_: String)).foreach { verb =>
          verb(url) ~> c.prefixAuthProxyRoute(
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
