package com.pagerduty.akka.http.aggregator.aggregator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.pagerduty.akka.http.aggregator.AggregatorUpstream
import com.pagerduty.akka.http.aggregator.support.{
  TestAuthConfig,
  TestUpstream
}
import com.pagerduty.akka.http.proxy.{HttpProxy, Upstream}
import com.pagerduty.akka.http.support.RequestMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}
import ujson.Js

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class TwoStepJsonHydrationAggregatorSpec
    extends TestKit(ActorSystem("TwoStepJsonHydrationAggregatorSpec"))
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

  "A TwoStepJsonHydrationAggregator" - {
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

    class TestAggregator
        extends TwoStepJsonHydrationAggregator[String, String] {
      override def handleIncomingRequest(incomingRequest: HttpRequest)
        : Either[HttpResponse, (AggregatorUpstream[String], HttpRequest)] =
        Right((upstream1, request1))

      override def handleJsonUpstreamResponse(
          upstreamResponse: HttpResponse,
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
        upstreamResponseMap(request2Key) should equal((response2, entityJson2))

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
  }
}
