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
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.{HttpProxy, Upstream}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike, Matchers}
import ujson.Js

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class OneStepJsonHydrationAggregatorSpec
    extends TestKit(ActorSystem("OneStepJsonHydrationAggregatorSpec"))
    with FreeSpecLike
    with Matchers
    with MockFactory
    with ScalaFutures
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

  "A OneStepJsonHydrationAggregator" - {
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
      override def handleIncomingRequestStateless(
          authConfig: HeaderAuthConfig
      )(incomingRequest: HttpRequest,
        authData: authConfig.AuthData): Either[HttpResponse, RequestMap] = {
        authData should equal(authData)
        Right(expectedRequests)
      }

      override def buildOutgoingJsonResponseStateless(
          upstreamResponses: Map[String, (HttpResponse, Js.Value)]
      ): HttpResponse = {
        upstreamResponses should equal(expectedResponses)
        expectedResponse
      }
    }

    val aggregator = new TestOneStepJsonHydrationAggregator

    "sends requests to upstreams and aggregates the responses" in {
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

    "fails whole request when one request fails" in {
      implicit val stubProxy =
        new HttpProxy[String](null, null)(null, null, null) {
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
      val request = HttpRequest(uri = "/fail-path")

      val response = aggregator.execute(ac)(request, testAuthData)
      response.failed.futureValue shouldBe a[RuntimeException]
    }
  }
}
