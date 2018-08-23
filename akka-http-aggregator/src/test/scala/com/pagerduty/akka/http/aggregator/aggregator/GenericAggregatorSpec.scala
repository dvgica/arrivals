package com.pagerduty.akka.http.aggregator.aggregator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.pagerduty.akka.http.aggregator.support.{
  TestAuthConfig,
  TestUpstream
}
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.{HttpProxy, Upstream}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class GenericAggregatorSpec
    extends TestKit(ActorSystem("AggregatorSpec"))
    with FreeSpecLike
    with Matchers
    with MockFactory
    with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  implicit val executionContext = ExecutionContext.global
  implicit val materializer = ActorMaterializer()

  val ac = new TestAuthConfig

  val testAuthData = "auth-data"

  val upstream1 = TestUpstream(1, "1")
  val upstream2 = TestUpstream(2, "2")

  "A GenericAggregator" - {
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

    class TestAggregator extends GenericAggregator[String, String, String] {
      override def handleIncomingRequest(
          authConfig: HeaderAuthConfig
      )(incomingRequest: HttpRequest,
        authData: authConfig.AuthData): HandlerResult = {
        authData should equal(testAuthData)
        Right((initialState, expectedRequests1))
      }

      def handleIntermediateResponse(state: String,
                                     responses: ResponseMap): HandlerResult = {
        state should equal(initialState)
        responses should equal(expectedResponseMap1)

        Right((intermediateState, expectedRequests2))
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

    "sends requests to upstreams and aggregates responses in multiple steps" in {
      implicit val stubProxy =
        new HttpProxy[String](null, null)(null, null, null) {
          override def request(
              request: HttpRequest,
              upstream: Upstream[String]): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(response1)
              case `upstream2` => Future.successful(response2)
            }
          }
        }

      val request = HttpRequest()

      val response =
        Await.result(aggregator.execute(ac)(request, testAuthData), 10.seconds)
      response should equal(expectedResponse)
    }
  }
}
