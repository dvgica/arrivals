package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future

class ProxyControllerSpec
    extends FreeSpecLike
    with Matchers
    with ScalatestRouteTest
    with MockFactory { outer =>

  "ProxyControllerSpec" - {
    val expectedResponse = HttpResponse(201)

    val httpStub = new HttpProxy[String](null, null)(null, null, null) {
      override def request(request: HttpRequest,
                           upstream: Upstream[String]): Future[HttpResponse] =
        Future.successful(expectedResponse)
    }
    val c = new ProxyController[String] {
      override def httpProxy = httpStub
    }
    val upstream = new CommonHostnameUpstream {
      val port = 123
      val metricsTag = "test"
    }

    "proxies unauthenticated routes" in {
      Seq(Get(_: String),
          Post(_: String),
          Put(_: String),
          Delete(_: String),
          Patch(_: String)).foreach { verb =>
        verb("/") ~> c.proxyRouteUnauthenticated(upstream) ~> check {
          handled shouldBe true
          response should equal(expectedResponse)
        }
      }
    }
  }
}
