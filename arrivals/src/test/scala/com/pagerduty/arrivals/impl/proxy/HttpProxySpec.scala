package com.pagerduty.arrivals.impl.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.metrics.NullMetrics
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future

class HttpProxySpec extends FreeSpecLike with Matchers with ScalaFutures {
  "An HttpProxy" - {
    val headerKey = "x-test-header"
    val headerValue = "test"
    val additionalHeader = RawHeader(headerKey, headerValue)

    val authority = Authority(Uri.Host("localhost"), 1234)

    val upstream = new Upstream[String] {
      val metricsTag = "test"

      def addressRequest(request: HttpRequest, addressingConfig: String): HttpRequest = {
        val uri = request.uri.withAuthority(authority)
        request.withUri(uri)
      }

      override def prepareRequestForDelivery(request: HttpRequest): HttpRequest =
        request.addHeader(additionalHeader)

      override def transformResponse(request: HttpRequest, response: HttpResponse): HttpResponse = {
        response.addHeader(request.getHeader(headerKey).get)
      }
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val metrics = NullMetrics
    val response = HttpResponse()

    "removes the Timeout-Header if it exists before proxying" in {
      val httpClient = (req: HttpRequest) => {
        if (req.headers.exists(_.is("timeout-access"))) {
          throw new Exception("The Timeout-Access header is not being removed as we expect")
        }

        Future.successful(response)
      }

      val p = new HttpProxy("localhost", httpClient)

      p(HttpRequest().withHeaders(RawHeader("Timeout-Access", "foo")), upstream)
    }

    "prepares the request before proxying" in {
      val httpClient = (req: HttpRequest) => {
        req.headers.find(_.is(headerKey)) match {
          case Some(h) if h.value() == headerValue => // it works!
          case _ =>
            throw new Exception("The front-end header is not being added as we expect")
        }

        Future.successful(response)
      }

      val p = new HttpProxy("localhost", httpClient)

      p(HttpRequest(), upstream)
    }

    "addresses the request before proxy" in {
      val httpClient = (req: HttpRequest) => {
        if (req.uri.authority != authority) {
          throw new Exception("Authority on proxied request not being set as expected")
        }
        Future.successful(response)
      }

      val p = new HttpProxy("localhost", httpClient)

      p(HttpRequest(), upstream)
    }

    "transforms responses for an upstream" in {
      val httpClient = (req: HttpRequest) => {
        Future.successful(response)
      }

      val p = new HttpProxy("localhost", httpClient)

      whenReady(p(HttpRequest(), upstream)) { response =>
        response.getHeader(headerKey).get.value shouldBe headerValue
      }
    }
  }
}
