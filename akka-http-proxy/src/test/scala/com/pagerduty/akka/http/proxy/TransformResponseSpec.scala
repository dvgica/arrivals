package com.pagerduty.akka.http.proxy

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import com.pagerduty.metrics.NullMetrics
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future

class TransformResponseSpec extends FreeSpecLike with Matchers {
  "The transformResponse() function" - {
    val headerKeyRequest = "x-test-header-from-request"
    val headerValueRequest = "test"
    val additionalHeaderRequest = RawHeader(headerKeyRequest, headerValueRequest)
    val headerKeyResponse = "x-test-header-response"
    val headerValueResponse = "test"
    val additionalHeaderResponse = RawHeader(headerKeyResponse, headerValueResponse)

    val upstream = new CommonHostnameUpstream {
      val port = 1234
      val metricsTag = "test"

      override def prepareRequestForDelivery(request: HttpRequest): HttpRequest = {
        request.addHeader(additionalHeaderRequest)
      }

      override def transformResponse(request: HttpRequest, response: HttpResponse): HttpResponse = {
        response.addHeader(additionalHeaderResponse)
        response.addHeader(request.getHeader(headerKeyRequest).get)
      }
    }

    import scala.concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val metrics = NullMetrics
    val response = HttpResponse()

    "write headers to the response" in {
      val httpClient = (req: HttpRequest) => {
        response.headers.find(_.is(headerKeyRequest)) match {
          case Some(h) if h.value() == headerValueRequest => // it works!
          case _ =>
            throw new Exception(
              "The request header is not being added as we expected")
        }
        Future.successful(response)
      }

      val p = new HttpProxy("localhost", httpClient)
      p.request(HttpRequest(), upstream)
    }
  }
}
