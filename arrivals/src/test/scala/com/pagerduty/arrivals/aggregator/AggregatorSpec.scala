package com.pagerduty.arrivals.aggregator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.stream.Materializer
import akka.testkit.TestKit
import com.pagerduty.arrivals.aggregator.support.{TestAuthConfig, TestUpstream}
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.proxy.{HttpProxy, HttpProxyLike}
import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class AggregatorSpec
    extends TestKit(ActorSystem("AggregatorSpec"))
    with AnyFreeSpecLike
    with Matchers
    with MockFactory
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val executionContext = ExecutionContext.global
  implicit val actorSystem = ActorSystem()
  implicit val materializer = Materializer.matFromSystem(actorSystem)

  val ac = new TestAuthConfig

  def buildDeps(proxy: HttpProxyLike[String]): AggregatorDependencies[String] =
    AggregatorDependencies(ac, proxy, executionContext, materializer)

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

    class TestAggregator extends Aggregator[String, String, String, String] {
      override def handleIncomingRequest(incomingRequest: HttpRequest, authData: String): HandlerResult = {
        authData should equal(testAuthData)

        if (incomingRequest.method == HttpMethods.GET) {
          Right((initialState, expectedRequests1))
        } else {
          Left(initialFailureResponse)
        }
      }

      def handleIntermediateResponse(state: String, responses: ResponseMap): HandlerResult = {
        state should equal(initialState)

        if (responses == expectedResponseMap1) {
          Right((intermediateState, expectedRequests2))
        } else {
          Left(intermediateFailureResponse)
        }
      }

      def intermediateResponseHandlers: Seq[ResponseHandler] =
        Seq(handleIntermediateResponse)

      def buildOutgoingResponse(state: String, upstreamResponses: ResponseMap): HttpResponse = {
        state should equal(intermediateState)
        upstreamResponses should equal(expectedResponseMap2)

        expectedResponse
      }
    }

    val aggregator = new TestAggregator

    "sends requests to upstreams and aggregates responses in multiple steps" in {
      implicit val stubProxy =
        new HttpProxyLike[String] {
          override def apply(request: HttpRequest, upstream: Upstream[String]): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(response1)
              case `upstream2` => Future.successful(response2)
            }
          }
        }

      val request = HttpRequest()

      val response =
        Await.result(aggregator(request, buildDeps(stubProxy), testAuthData), 10.seconds)
      response should equal(expectedResponse)
    }

    "short-circuits if the initial handler returns a response" in {
      implicit val stubProxy =
        new HttpProxy[String](null, null, null)(null, null, null)

      val request = HttpRequest(HttpMethods.POST)

      val response =
        Await.result(aggregator(request, buildDeps(stubProxy), testAuthData), 10.seconds)
      response should equal(initialFailureResponse)
    }

    "short-circuits if one of the intermediate handlers returns a response" in {
      implicit val stubProxy =
        new HttpProxyLike[String] {
          override def apply(request: HttpRequest, upstream: Upstream[String]): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(failureResponse1)
              case `upstream2` => Future.successful(response2)
            }
          }
        }

      val request = HttpRequest()

      val response =
        Await.result(aggregator(request, buildDeps(stubProxy), testAuthData), 10.seconds)
      response should equal(intermediateFailureResponse)
    }
  }
}
