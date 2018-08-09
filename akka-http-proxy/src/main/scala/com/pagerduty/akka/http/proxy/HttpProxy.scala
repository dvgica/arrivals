package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.headers.Connection
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.pagerduty.akka.http.support.MetadataLogging
import com.pagerduty.metrics.{Metrics, Stopwatch}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object HttpProxy {
  val TimeoutAccessHeaderName = "Timeout-Access"
  val KeepAliveHeaderValue = "keep-alive"
}

class HttpProxy[AddressingConfig](
    addressingConfig: AddressingConfig,
    httpClient: HttpRequest => Future[HttpResponse],
    proxyRequestModifier: Option[HttpRequest => HttpRequest] = None
)(implicit ec: ExecutionContext, materializer: Materializer, metrics: Metrics)
    extends MetadataLogging {
  import HttpProxy._

  def request(request: HttpRequest,
              upstream: Upstream[AddressingConfig]): Future[HttpResponse] = {
    val targetedRequest =
      upstream.addressRequestWithOverrides(request, addressingConfig)

    val proxyRequest =
      targetedRequest
        .removeHeader(TimeoutAccessHeaderName) // Akka HTTP server adds the Timeout-Access for internal reasons, but it should not be proxied
        .removeHeader(Connection.name) // remove any existing Connection header
        .addHeader(Connection(KeepAliveHeaderValue)) // add a Connection: keep-alive header

    val modifiedProxyRequest = proxyRequestModifier match {
      case Some(modifier) => modifier(proxyRequest)
      case None => proxyRequest
    }

    val stopwatch = Stopwatch.start()
    val response = httpClient(modifiedProxyRequest)

    response.flatMap { r =>
      val elapsed = stopwatch.elapsed().toMicros.toInt
      metrics.histogram("upstream_response_time",
                        elapsed,
                        "upstream" -> upstream.metricsTag)

      r.entity.withoutSizeLimit().toStrict(20.seconds).map { e =>
        r.withEntity(e)
      }
    }
  }
}
