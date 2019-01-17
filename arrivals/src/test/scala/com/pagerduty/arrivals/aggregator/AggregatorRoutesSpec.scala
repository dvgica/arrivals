package com.pagerduty.arrivals.aggregator

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.ArrivalsContext
import com.pagerduty.arrivals.api.auth.AuthFailedReason
import com.pagerduty.arrivals.api.filter.{RequestFilter, ResponseFilter}
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.proxy.HttpProxyLike
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future
import scala.util.{Success, Try}

class AggregatorRoutesSpec extends FreeSpecLike with Matchers with MockFactory with ScalatestRouteTest {

  val testAuthData = "auth-data"

  class TestAuthConfig extends HeaderAuthConfig {
    type AuthData = String
    type Permission = String

    def authenticate(request: HttpRequest)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = {
      if (request.uri.toString.contains("failed-auth")) {
        Future.successful(Success(None))
      } else {
        Future.successful(Success(Some(testAuthData)))
      }
    }

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

  "AggregatorRoutes" - {
    val ac = new TestAuthConfig
    val expectedResponse = HttpResponse(StatusCodes.NotModified)

    implicit val context = ArrivalsContext("localhost")

    def buildController(stubHttpProxy: HttpProxyLike[String] = null): AggregatorRoutes[TestAuthConfig] = {
      new AggregatorRoutes[TestAuthConfig](ac)
    }

    "with a simple test Aggregator" - {

      class TestAggregator extends Aggregator[String, String, String, String] {
        def handleIncomingRequest(incomingRequest: HttpRequest, authData: String): HandlerResult = ???

        def intermediateResponseHandlers: Seq[ResponseHandler] = ???

        def buildOutgoingResponse(accumulatedState: String, upstreamResponses: ResponseMap): HttpResponse = ???

        override def apply(
            authedRequest: HttpRequest,
            deps: AggregatorDependencies[String],
            authData: String
          ): Future[HttpResponse] = {
          authData should equal(testAuthData)
          Future.successful(expectedResponse)
        }
      }

      val aggregator = new TestAggregator

      "does not execute the aggregator and completes with Forbidden when auth fails" in {
        val c = buildController()
        Get("/failed-auth") ~> c.prefixAggregatorRoute("failed-auth", aggregator) ~> check {
          handled shouldBe true
          response.status should equal(StatusCodes.Forbidden)
        }
      }

      "matches the path before doing anything" in {
        val c = buildController()
        Get("/wrong-path") ~> c.prefixAggregatorRoute("aggregator", aggregator) ~> check {
          handled shouldBe false
        }
      }

      "filters, authenticates and proxies requests" in {
        val c = buildController()

        val requestTransformer = new RequestFilter[String] {
          def apply(request: HttpRequest, authData: String): Future[Right[Nothing, HttpRequest]] = {
            authData should equal(testAuthData)

            Future.successful(Right(request.withUri("transformed")))
          }
        }

        val transformedResponse = HttpResponse(StatusCodes.MethodNotAllowed)

        val responseTransformer = new ResponseFilter[String] {
          def apply(request: HttpRequest, response: HttpResponse, authData: String): Future[HttpResponse] = {
            authData should equal(testAuthData)
            response should equal(expectedResponse)

            Future.successful(transformedResponse)
          }
        }

        Get("/") ~> c.aggregatorRoute(
          aggregator,
          requestTransformer,
          responseTransformer
        ) ~> check {
          handled shouldBe true
          response should equal(transformedResponse)
        }
      }
    }
  }
}
