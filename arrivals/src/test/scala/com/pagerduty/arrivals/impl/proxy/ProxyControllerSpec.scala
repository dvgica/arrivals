package com.pagerduty.arrivals.impl.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.arrivals.api.RequestHandler
import com.pagerduty.arrivals.api.proxy.Upstream
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class ProxyControllerSpec
    extends FreeSpecLike
    with Matchers
    with ScalatestRouteTest
    with MockFactory { outer =>

  "ProxyControllerSpec" - {
    val expectedResponse = HttpResponse(201)

    val httpStub = new HttpProxy[String](null, null)(null, null, null) {
      override def apply(request: HttpRequest,
                         upstream: Upstream[String],
                         t: Any): Future[HttpResponse] =
        Future.successful(expectedResponse)
    }
    val c = new ProxyController[String] {
      override def httpProxy = httpStub
      override def proxyRequestHandler =
        new ProxyRequestHandler[String] {
          implicit val executionContext = ExecutionContext.global
        }
    }
    val upstream = new Upstream[String] {
      val metricsTag = "test"
      def addressRequest(request: HttpRequest,
                         addressingConfig: String): HttpRequest = request
    }

    "proxies routes" in {
      Seq(Get(_: String),
          Post(_: String),
          Put(_: String),
          Delete(_: String),
          Patch(_: String)).foreach { verb =>
        verb("/") ~> c.proxyRoute(upstream) ~> check {
          handled shouldBe true
          response should equal(expectedResponse)
        }
      }
    }
  }
}
