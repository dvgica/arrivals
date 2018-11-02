package com.pagerduty.arrivals.impl.aggregator

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.impl.aggregator.support.TestAuthConfig
import com.pagerduty.arrivals.api.aggregator.AggregatorDependencies
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.impl.auth.RequestAuthenticator
import com.pagerduty.arrivals.impl.headerauth.HeaderAuthenticator
import com.pagerduty.arrivals.impl.proxy.HttpProxy
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class AggregatorControllerSpec extends FreeSpecLike with Matchers with MockFactory with ScalatestRouteTest {

  "AggregatorController" - {
    val ac = new TestAuthConfig
    val testAuthData = "test-auth-data"
    val expectedResponse = HttpResponse(StatusCodes.NotModified)

    def buildController(stubHttpProxy: HttpProxy[String] = null): AggregatorController[TestAuthConfig, String] = {
      val c = new AggregatorController[TestAuthConfig, String] {
        val authConfig = ac
        val httpProxy = stubHttpProxy
        val aggregatorRequestHandler =
          new AggregatorRequestHandler[String, TestAuthConfig#AuthData] {
            val executionContext = ExecutionContext.global
          }
        val headerAuthenticator = new HeaderAuthenticator {
          def requestAuthenticator: RequestAuthenticator = ???

          override def addAndRequireAuthHeader(
              authConfig: HeaderAuthConfig
            )(request: HttpRequest,
              requiredPermission: Option[authConfig.Permission] = None
            )(handler: (HttpRequest, authConfig.AuthData) => Future[HttpResponse]
            )(implicit reqMeta: RequestMetadata
            ): Future[HttpResponse] = {
            if (request.uri.path.toString.contains("failed-auth")) {
              Future.successful(HttpResponse(Unauthorized))
            } else {
              handler(request, testAuthData.asInstanceOf[authConfig.AuthData])
            }
          }
        }

        val executionContext = ExecutionContext.global
        val materializer = ActorMaterializer()
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

      "does not execute the aggregator and returns Unauthorized when auth fails" in {
        val c = buildController()
        Get("/failed-auth") ~> c.prefixAggregatorRoute("failed-auth", aggregator) ~> check {
          handled shouldBe true
          response.status should equal(Unauthorized)
        }
      }

      "matches the path before doing anything" in {
        val c = buildController()
        Get("/wrong-path") ~> c.prefixAggregatorRoute("aggregator", aggregator) ~> check {
          handled shouldBe false
        }
      }
    }
  }
}
