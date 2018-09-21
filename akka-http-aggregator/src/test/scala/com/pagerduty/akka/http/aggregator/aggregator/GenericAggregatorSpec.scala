package com.pagerduty.akka.http.aggregator.aggregator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{
  HttpMethods,
  HttpRequest,
  HttpResponse,
  StatusCodes
}
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.pagerduty.akka.http.aggregator.support.{
  TestAuthConfig,
  TestUpstream
}
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.{HttpProxy, Upstream}
import com.pagerduty.akka.http.support.RequestMetadata
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
  implicit val reqMeta = RequestMetadata.fromRequest(HttpRequest())

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
    val failureResponse1 = HttpResponse(StatusCodes.InternalServerError)

    val expectedRequests1 = Map(request1Key -> (upstream1, request1))
    val expectedResponseMap1 = Map(request1Key -> (response1, entity1))

    val expectedRequests2 = Map(request2Key -> (upstream2, request2))
    val expectedResponseMap2 = Map(request2Key -> (response2, entity2))

    val expectedResponse = HttpResponse(entity = "aggregated data")

    val initialState = "initial"
    val intermediateState = "intermediate"

    val initialFailureResponse = HttpResponse(StatusCodes.MethodNotAllowed)
    val intermediateFailureResponse = HttpResponse(StatusCodes.BadGateway)

    class TestAggregator
        extends GenericAggregator[String, String, String, String] {
      override def handleIncomingRequest(incomingRequest: HttpRequest,
                                         authData: String): HandlerResult = {
        authData should equal(testAuthData)

        if (incomingRequest.method == HttpMethods.GET) {
          Right((initialState, expectedRequests1))
        } else {
          Left(initialFailureResponse)
        }
      }

      def handleIntermediateResponse(state: String,
                                     responses: ResponseMap): HandlerResult = {
        state should equal(initialState)

        if (responses == expectedResponseMap1) {
          Right((intermediateState, expectedRequests2))
        } else {
          Left(intermediateFailureResponse)
        }
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
          override def request(request: HttpRequest,
                               upstream: Upstream[String])(
              implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
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

    "short-circuits if the initial handler returns a response" in {
      implicit val stubProxy =
        new HttpProxy[String](null, null)(null, null, null)

      val request = HttpRequest(HttpMethods.POST)

      val response =
        Await.result(aggregator.execute(ac)(request, testAuthData), 10.seconds)
      response should equal(initialFailureResponse)
    }

    "short-circuits if one of the intermediate handlers returns a response" in {
      implicit val stubProxy =
        new HttpProxy[String](null, null)(null, null, null) {
          override def request(request: HttpRequest,
                               upstream: Upstream[String])(
              implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(failureResponse1)
              case `upstream2` => Future.successful(response2)
            }
          }
        }

      val request = HttpRequest()

      val response =
        Await.result(aggregator.execute(ac)(request, testAuthData), 10.seconds)
      response should equal(intermediateFailureResponse)
    }
  }
}
