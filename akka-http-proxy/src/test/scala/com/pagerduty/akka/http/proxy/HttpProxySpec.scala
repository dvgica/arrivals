package com.pagerduty.akka.http.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import com.pagerduty.metrics.NullMetrics
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future

class HttpProxySpec extends FreeSpecLike with Matchers {
  "An HttpProxy" - {
    val headerKey = "x-test-header"
    val headerValue = "test"
    val additionalHeader = RawHeader(headerKey, headerValue)

    val upstream = new CommonHostnameUpstream {
      val port = 1234
      val metricsTag = "test"
      override def prepareRequestForDelivery(
          request: HttpRequest): HttpRequest =
        request.addHeader(additionalHeader)
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val metrics = NullMetrics
    val response = HttpResponse()

    "removes the Timeout-Header if it exists before proxying" in {
      val httpClient = (req: HttpRequest) => {
        if (req.headers.exists(_.is("timeout-access"))) {
          throw new Exception(
            "The Timeout-Access header is not being removed as we expect")
        }

        Future.successful(response)
      }

      val p = new HttpProxy("localhost", httpClient)

      p.request(HttpRequest().withHeaders(RawHeader("Timeout-Access", "foo")),
                upstream)
    }

    "prepares the request before proxying" in {
      val httpClient = (req: HttpRequest) => {
        req.headers.find(_.is(headerKey)) match {
          case Some(h) if h.value() == headerValue => // it works!
          case _ =>
            throw new Exception(
              "The front-end header is not being added as we expect")
        }

        Future.successful(response)
      }

      val p = new HttpProxy("localhost", httpClient)

      p.request(HttpRequest(), upstream)
    }

    "proxies the request to the given authority" in {
      val authority = Authority(Uri.Host("localhost"), 1234)
      val httpClient = (req: HttpRequest) => {
        if (req.uri.authority != authority) {
          throw new Exception(
            "Authority on proxied request not being set as expected")
        }
        Future.successful(response)
      }

      val p = new HttpProxy("localhost", httpClient)

      p.request(HttpRequest(), upstream)
    }
  }
}
