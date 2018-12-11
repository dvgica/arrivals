package com.pagerduty.arrivals.impl.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.arrivals.api.filter.{RequestFilter, RequestFilterOutput, ResponseFilter}
import com.pagerduty.arrivals.api.proxy.Upstream
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future

class ProxyControllerSpec extends FreeSpecLike with Matchers with ScalatestRouteTest with MockFactory { outer =>

  "ProxyControllerSpec" - {
    val expectedResponse = HttpResponse(201)

    val httpStub = new HttpProxy[String](null, null)(null, null, null) {
      override def apply(request: HttpRequest, upstream: Upstream[String]): Future[HttpResponse] =
        Future.successful(expectedResponse)
    }
    val c = new ProxyController[String] {
      override def httpProxy = httpStub
    }
    val upstream = new Upstream[String] {
      val metricsTag = "test"
      def addressRequest(request: HttpRequest, addressingConfig: String): HttpRequest = request
    }

    "filters and proxies routes" in {
      val requestTransformer = new RequestFilter[Any] {
        def apply(request: HttpRequest, t: Any): RequestFilterOutput = {
          Future.successful(Right(request.withUri("transformed")))
        }
      }

      val transformedResponse = HttpResponse(StatusCodes.MethodNotAllowed)

      val responseTransformer = new ResponseFilter[Any] {
        def apply(request: HttpRequest, response: HttpResponse, t: Any): Future[HttpResponse] = {
          response should equal(expectedResponse)

          Future.successful(transformedResponse)
        }
      }

      Seq(Get(_: String), Post(_: String), Put(_: String), Delete(_: String), Patch(_: String)).foreach { verb =>
        verb("/") ~> c.proxyRoute(upstream, requestTransformer, responseTransformer) ~> check {
          handled shouldBe true
          response should equal(transformedResponse)
        }
      }
    }
  }
}
