package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Promise

class ProxyControllerSpec
    extends FreeSpecLike
    with Matchers
    with ScalatestRouteTest
    with MockFactory { outer =>

  "ProxyControllerSpec" - {
    val httpStub = stub[HttpProxy]
    val c = new ProxyController {
      override def httpProxy = httpStub
    }
    val upstream = new LocalPortUpstream {
      val localPort = 123
      val metricsTag = "test"
    }

    "proxies unauthenticated routes" in {
      val expectedResponse = HttpResponse(201)
      (c.httpProxy.request _)
        .when(*, upstream)
        .returns(Promise.successful(expectedResponse).future)

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
