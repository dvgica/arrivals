package com.pagerduty.arrivals.aggregator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.testkit.TestKit
import com.pagerduty.arrivals.aggregator.support.{TestAuthConfig, TestUpstream}
import com.pagerduty.arrivals.api.aggregator.AggregatorUpstream
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.proxy.HttpProxyLike
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import ujson.Js

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class OneStepJsonHydrationAggregatorSpec
    extends TestKit(ActorSystem("OneStepJsonHydrationAggregatorSpec"))
    with AnyFreeSpecLike
    with Matchers
    with MockFactory
    with ScalaFutures
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

    val expectedRequests: Map[String, (AggregatorUpstream[String], HttpRequest)] =
      Map(reqKey1 -> (upstream1, request1), reqKey2 -> (upstream2, request2))
    val expectedResponse = HttpResponse(entity = "aggregated data")

    val expectedResponses = Map(reqKey1 -> (response1, entityJson1), reqKey2 -> (response2, entityJson2))

    class TestOneStepJsonHydrationAggregator extends OneStepJsonHydrationAggregator[String, String] {
      override def handleIncomingRequestStateless(
          incomingRequest: HttpRequest,
          authData: String
        ): Either[HttpResponse, RequestMap] = {
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

    "fails whole request when one request fails" in {
      implicit val stubProxy =
        new HttpProxyLike[String] {
          override def apply(request: HttpRequest, upstream: Upstream[String]): Future[HttpResponse] = {
            upstream match {
              case `upstream1` => Future.successful(response1)
              case `upstream2` =>
                Future.failed(new RuntimeException("simulated exception"))
            }
          }
        }
      val request = HttpRequest(uri = "/fail-path")

      val response = aggregator(request, buildDeps(stubProxy), testAuthData)
      response.failed.futureValue shouldBe a[RuntimeException]
    }
  }
}
