package com.pagerduty.arrivals.impl.authproxy

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthFailedReason
import com.pagerduty.arrivals.api.filter.{RequestFilter, RequestFilterOutput, ResponseFilter}
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.api.proxy.HttpProxy
import com.pagerduty.metrics.NullMetrics
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future
import scala.util.{Success, Try}

class AuthProxyControllerSpec extends FreeSpecLike with Matchers with ScalatestRouteTest with MockFactory { outer =>
  implicit val reqMeta = RequestMetadata(None)

  val testAuthData = "auth-data"

  class TestAuthConfig extends HeaderAuthConfig {
    type Cred = String
    type AuthData = String
    type Permission = String

    def extractCredentials(request: HttpRequest)(implicit reqMeta: RequestMetadata): List[Cred] = List("credential")

    def authenticate(credential: Cred)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] =
      Future.successful(Success(Some(testAuthData)))

    def authDataGrantsPermission(
        authData: AuthData,
        request: HttpRequest,
        permission: Option[Permission]
      )(implicit reqMeta: RequestMetadata
      ): Option[AuthFailedReason] = None

    def dataToAuthHeader(data: AuthData)(implicit reqMeta: RequestMetadata): HttpHeader =
      RawHeader(authHeaderName, "true")
    def authHeaderName: String = "X-Authed"
  }

  "AuthProxyController" - {
    val testAuthConfig = new TestAuthConfig

    val expectedResponse = HttpResponse(201)
    val expectedTransformedResponse = HttpResponse(302)

    val proxyStub = new HttpProxy[String] {
      override def apply(
          request: HttpRequest,
          upstream: Upstream[String]
        )(implicit reqMeta: RequestMetadata
        ): Future[HttpResponse] = {
        if (request.headers.exists(_.is(testAuthConfig.authHeaderName.toLowerCase))) {
          if (request.uri.toString.contains("transformed")) {
            Future.successful(expectedTransformedResponse)
          } else {
            Future.successful(expectedResponse)
          }
        } else {
          Future.successful(HttpResponse(StatusCodes.Forbidden))
        }
      }

    }

    val c = new AuthProxyController[TestAuthConfig, String] {
      implicit val executionContext =
        scala.concurrent.ExecutionContext.Implicits.global
      val authConfig = testAuthConfig
      def httpProxy = proxyStub
      val metrics = NullMetrics
    }
    val upstream = new Upstream[String] {
      def addressRequest(request: HttpRequest, addressingConfig: String): HttpRequest = request

      val metricsTag = "test"
    }

    "filters, authenticates and proxies requests" in {
      val requestTransformer = new RequestFilter[Option[String]] {
        def apply(request: HttpRequest, optAuthData: Option[String]): RequestFilterOutput = {
          optAuthData should equal(Some(testAuthData))

          Future.successful(Right(request.withUri("transformed")))
        }
      }

      val transformedResponse = HttpResponse(StatusCodes.MethodNotAllowed)

      val responseTransformer = new ResponseFilter[Option[String]] {
        def apply(request: HttpRequest, response: HttpResponse, optAuthData: Option[String]): Future[HttpResponse] = {
          optAuthData should equal(Some(testAuthData))
          response should equal(expectedTransformedResponse)

          Future.successful(transformedResponse)
        }
      }

      Get("/") ~> c.authProxyRoute(
        upstream,
        requestTransformer,
        responseTransformer
      ) ~> check {
        handled shouldBe true
        response should equal(transformedResponse)
      }
    }

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
