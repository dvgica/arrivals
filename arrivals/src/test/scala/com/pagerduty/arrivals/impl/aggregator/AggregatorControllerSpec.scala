package com.pagerduty.arrivals.impl.aggregator

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthFailedReason
import com.pagerduty.arrivals.api.filter.{RequestFilter, RequestFilterOutput, ResponseFilter}
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.impl.proxy.HttpProxy
import com.pagerduty.metrics.NullMetrics
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class AggregatorControllerSpec extends FreeSpecLike with Matchers with MockFactory with ScalatestRouteTest {

  val testAuthData = "auth-data"

  class TestAuthConfig extends HeaderAuthConfig {
    type Cred = String
    type AuthData = String
    type Permission = String

    def extractCredentials(request: HttpRequest)(implicit reqMeta: RequestMetadata): List[Cred] =
      if (request.uri.toString.contains("failed-auth")) {
        List()
      } else {
        List("credential")
      }

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

  "AggregatorController" - {
    val ac = new TestAuthConfig
    val expectedResponse = HttpResponse(StatusCodes.NotModified)

    def buildController(stubHttpProxy: HttpProxy[String] = null): AggregatorController[TestAuthConfig, String] = {
      val c = new AggregatorController[TestAuthConfig, String] {
        val authConfig = ac
        val httpProxy = stubHttpProxy
        val executionContext = ExecutionContext.global
        val materializer = ActorMaterializer()
        val metrics = NullMetrics
      }

      c
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
          def apply(request: HttpRequest, authData: String): RequestFilterOutput = {
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
