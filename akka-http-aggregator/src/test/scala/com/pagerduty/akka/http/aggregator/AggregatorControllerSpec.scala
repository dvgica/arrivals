package com.pagerduty.akka.http.aggregator

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.{ActorMaterializer, Materializer}
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.aggregator.aggregator.Aggregator
import com.pagerduty.akka.http.aggregator.support.TestAuthConfig
import com.pagerduty.akka.http.proxy.HttpProxy
import com.pagerduty.akka.http.requestauthentication.RequestAuthenticator
import com.pagerduty.akka.http.support.RequestMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class AggregatorControllerSpec
    extends FreeSpecLike
    with Matchers
    with MockFactory
    with ScalatestRouteTest {

  "AggregatorController" - {
    val ac = new TestAuthConfig
    val testAuthData = "test-auth-data"
    val expectedResponse = HttpResponse(StatusCodes.NotModified)

    def buildController(stubHttpProxy: HttpProxy[String] = null)
      : AggregatorController[TestAuthConfig, String] = {
      val c = new AggregatorController[TestAuthConfig, String] {
        val authConfig = ac
        val httpProxy = stubHttpProxy
        val headerAuthenticator = new HeaderAuthenticator {
          def requestAuthenticator: RequestAuthenticator = ???

          override def addAndRequireAuthHeader(
              authConfig: HeaderAuthConfig
          )(request: HttpRequest,
            stripAuthorizationHeader: Boolean = true,
            requiredPermission: Option[authConfig.Permission] = None)(
              handler: (HttpRequest,
                        authConfig.AuthData) => Future[HttpResponse])(
              implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
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

      class TestAggregator extends Aggregator[String, String] {
        def execute(authConfig: HeaderAuthConfig)(authedRequest: HttpRequest,
                                                  authData: String)(
            implicit httpProxy: HttpProxy[String],
            executionContext: ExecutionContext,
            materializer: Materializer,
            reqMeta: RequestMetadata): Future[HttpResponse] = {
          authData should equal(testAuthData)
          Future.successful(expectedResponse)
        }
      }

      val aggregator = new TestAggregator

      "does not execute the aggregator and returns Unauthorized when auth fails" in {
        val c = buildController()
        Get("/failed-auth") ~> c.prefixAggregatorRoute("failed-auth",
                                                       aggregator) ~> check {
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
