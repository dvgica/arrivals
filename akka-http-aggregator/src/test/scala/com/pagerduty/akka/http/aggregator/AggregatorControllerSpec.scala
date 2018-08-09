package com.pagerduty.akka.http.aggregator

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.aggregator.aggregator.{
  Aggregator,
  OneStepJsonHydrationAggregator,
  TwoStepJsonHydrationAggregator
}
import com.pagerduty.akka.http.proxy.{
  CommonHostnameUpstream,
  HttpProxy,
  Upstream
}
import com.pagerduty.akka.http.requestauthentication.RequestAuthenticator
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationConfig
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationData.AuthFailedReason
import com.pagerduty.akka.http.support.RequestMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}
import ujson.Js

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AggregatorControllerSpec
    extends FreeSpecLike
    with Matchers
    with MockFactory
    with ScalatestRouteTest {

  class TestAuthConfig extends HeaderAuthConfig {
    type Cred = String
    type AuthData = String
    type Permission = String
    type AuthHeader = RawHeader

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
        implicit reqMeta: RequestMetadata): AuthHeader = ???
    def authHeaderName: String = ???
  }

  case class TestUpstream(port: Int, metricsTag: String)
      extends CommonHostnameUpstream
      with AggregatorUpstream[String] {
    override def prepareAggregatorRequestForDelivery(
        authConfig: HeaderAuthConfig,
        request: HttpRequest,
        modelRequest: HttpRequest
    ): HttpRequest =
      request
  }

  "AggregatorController" - {
    val ac = new TestAuthConfig

    val expectedAuthData = "auth-data"
    val upstream1 = TestUpstream(1, "1")
    val upstream2 = TestUpstream(2, "2")

    val authToken = "auth-token"

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
              handler(request,
                      expectedAuthData.asInstanceOf[authConfig.AuthData])
            }
          }
        }

        val executionContext = ExecutionContext.global
        val materializer = ActorMaterializer()
      }

      c
    }

    "with a SimpleOneStepAggregator" - {
      val request1 = HttpRequest(uri = "/req1")
      val request2 = HttpRequest(uri = "/req2")

      val entity1 = "{ \"entity\": 1 }"
      val entity2 = "{ \"entity\": 2 }"

      val entityJson1 = ujson.read(entity1)
      val entityJson2 = ujson.read(entity2)

      val response1 = HttpResponse(entity = entity1)
      val response2 = HttpResponse(entity = entity2)

      val reqKey1 = "req1"
      val reqKey2 = "req2"

      val expectedRequests
        : Map[String, (AggregatorUpstream[String], HttpRequest)] =
        Map(reqKey1 -> (upstream1, request1), reqKey2 -> (upstream2, request2))
      val expectedResponse = HttpResponse(entity = "aggregated data")

      val expectedResponses = Map(reqKey1 -> (response1, entityJson1),
                                  reqKey2 -> (response2, entityJson2))

      class TestOneStepJsonHydrationAggregator
          extends OneStepJsonHydrationAggregator[String] {
        def handleIncomingRequestStateless(
            authConfig: AuthenticationConfig
        )(incomingRequest: HttpRequest, authData: authConfig.AuthData)
          : Map[String, (AggregatorUpstream[String], HttpRequest)] = {
          authData should equal(expectedAuthData)
          expectedRequests
        }

        def buildOutgoingJsonResponseStateless(
            upstreamResponses: Map[String, (HttpResponse, Js.Value)]
        ): HttpResponse = {
          upstreamResponses should equal(expectedResponses)
          expectedResponse
        }
      }

      val aggregator = new TestOneStepJsonHydrationAggregator

      "sends requests to upstreams and aggregates the responses when auth succeeds" in {
        val stubProxy = new HttpProxy[String](null, null)(null, null, null) {
          override def request(
              request: HttpRequest,
              upstream: Upstream[String]): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(response1)
              case `upstream2` => Future.successful(response2)
            }
          }
        }
        val c = buildController(stubProxy)

        Get("/") ~> c.prefixAggregatorRoute("", aggregator) ~> check {
          handled shouldBe true
          response should equal(expectedResponse)
        }
      }

      "does not send requests to upstreams and returns Unauthorized when auth fails" in {
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

      "fails whole request when one request fails" in {
        val stubProxy = new HttpProxy[String](null, null)(null, null, null) {
          override def request(
              request: HttpRequest,
              upstream: Upstream[String]): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(response1)
              case `upstream2` =>
                Future.failed(new RuntimeException("simulated exception"))
            }
          }
        }
        val c = buildController(stubProxy)

        Get("/fail-path") ~> c.prefixAggregatorRoute("fail-path", aggregator) ~> check {
          handled shouldBe true
          response.status should equal(InternalServerError)
        }
      }
    }

    "with a TwoStepJsonHydrationAggregator" - {
      val request1Key = "request1"
      val request1 = HttpRequest(uri = "/req1")
      val request2Key = "request2"
      val request2 = HttpRequest(uri = "/req2")

      val entity1 = "{ \"entity\": 1 }"
      val entity2 = "{ \"entity\": 2 }"

      val entityJson1 = ujson.read(entity1)
      val entityJson2 = ujson.read(entity2)

      val response1 = HttpResponse(entity = entity1)
      val response2 = HttpResponse(entity = entity2)

      val expectedRequests1 = Map(request1Key -> (upstream1, request1))
      val expectedResponseMap1 = Map(request1Key -> (response1, entity1))

      val expectedRequests2 = Map(request2Key -> (upstream2, request2))
      val expectedResponseMap2 = Map(request2Key -> (response2, entity2))

      val expectedResponse = HttpResponse(entity = "aggregated data")

      val initialState = "initial"
      val intermediateState = "intermediate"

      class TestAggregator extends TwoStepJsonHydrationAggregator[String] {
        def handleIncomingRequest(incomingRequest: HttpRequest)
          : (AggregatorUpstream[String], HttpRequest) =
          (upstream1, request1)

        def handleJsonUpstreamResponse(upstreamResponse: HttpResponse,
                                       upstreamJson: Js.Value): RequestMap = {
          upstreamResponse should equal(response1)
          upstreamJson should equal(entityJson1)

          expectedRequests2
        }

        def buildOutgoingJsonResponse(
            initialUpstreamJson: Js.Value,
            upstreamResponseMap: Map[String, (HttpResponse, Js.Value)]
        ): HttpResponse = {
          initialUpstreamJson should equal(entityJson1)
          upstreamResponseMap should have size (1)
          upstreamResponseMap(request2Key) should equal(
            (response2, entityJson2))

          expectedResponse
        }
      }

      val aggregator = new TestAggregator

      "sends requests to upstreams and aggregates responses in multiple steps when auth succeeds" in {
        val stubProxy = new HttpProxy[String](null, null)(null, null, null) {
          override def request(
              request: HttpRequest,
              upstream: Upstream[String]): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(response1)
              case `upstream2` => Future.successful(response2)
            }
          }
        }
        val c = buildController(stubProxy)

        Get("/") ~> c.prefixAggregatorRoute("", aggregator) ~> check {
          handled shouldBe true
          response should equal(expectedResponse)
        }
      }
    }

    "with an Aggregator" - {
      val request1Key = "request1"
      val request1 = HttpRequest(uri = "/req1")
      val request2Key = "request2"
      val request2 = HttpRequest(uri = "/req2")

      val entity1 = "resp1"
      val entity2 = "resp2"

      val response1 = HttpResponse(entity = entity1)
      val response2 = HttpResponse(entity = entity2)

      val expectedRequests1 = Map(request1Key -> (upstream1, request1))
      val expectedResponseMap1 = Map(request1Key -> (response1, entity1))

      val expectedRequests2 = Map(request2Key -> (upstream2, request2))
      val expectedResponseMap2 = Map(request2Key -> (response2, entity2))

      val expectedResponse = HttpResponse(entity = "aggregated data")

      val initialState = "initial"
      val intermediateState = "intermediate"

      class TestAggregator extends Aggregator[String, String, String] {
        def handleIncomingRequest(
            authConfig: AuthenticationConfig
        )(incomingRequest: HttpRequest,
          authData: authConfig.AuthData): (String, RequestMap) = {
          authData should equal(expectedAuthData)
          (initialState, expectedRequests1)
        }

        def handleIntermediateResponse(
            state: String,
            responses: ResponseMap): (String, RequestMap) = {
          state should equal(initialState)
          responses should equal(expectedResponseMap1)

          (intermediateState, expectedRequests2)
        }

        def intermediateResponseHandlers: Seq[ResponseHandler] =
          Seq(handleIntermediateResponse)

        def buildOutgoingResponse(
            state: String,
            upstreamResponses: ResponseMap): HttpResponse = {
          state should equal(intermediateState)
          upstreamResponses should equal(expectedResponseMap2)

          expectedResponse
        }
      }

      val aggregator = new TestAggregator

      "sends requests to upstreams and aggregates responses in multiple steps when auth succeeds" in {
        val stubProxy = new HttpProxy[String](null, null)(null, null, null) {
          override def request(
              request: HttpRequest,
              upstream: Upstream[String]): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(response1)
              case `upstream2` => Future.successful(response2)
            }
          }
        }
        val c = buildController(stubProxy)

        Get("/") ~> c.prefixAggregatorRoute("", aggregator) ~> check {
          handled shouldBe true
          response should equal(expectedResponse)
        }
      }
    }
  }
}
