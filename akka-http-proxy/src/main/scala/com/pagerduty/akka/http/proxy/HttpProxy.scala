package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.pagerduty.akka.http.proxy.responder.RequestResponder
import com.pagerduty.akka.http.support.MetadataLogging
import com.pagerduty.metrics.{Metrics, Stopwatch}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object HttpProxy {
  val TimeoutAccessHeaderName = "Timeout-Access"
}

class HttpProxy[AddressingConfig](
    addressingConfig: AddressingConfig,
    httpClient: HttpRequest => Future[HttpResponse],
    entityConsumptionTimeout: FiniteDuration = 20.seconds
)(implicit ec: ExecutionContext, materializer: Materializer, metrics: Metrics)
    extends RequestResponder[Upstream[AddressingConfig], Any]
    with MetadataLogging {
  import HttpProxy._

  def apply(request: HttpRequest,
            upstream: Upstream[AddressingConfig],
            data: Option[Any]): Future[HttpResponse] = {
    val addressedRequest =
      upstream
        .addressRequestWithOverrides(request, addressingConfig)
        .removeHeader(TimeoutAccessHeaderName) // Akka HTTP server adds the Timeout-Access for internal reasons, but it should not be proxied

    val preparedProxyRequest =
      upstream.prepareRequestForDelivery(addressedRequest)

    val stopwatch = Stopwatch.start()
    val response = httpClient(preparedProxyRequest)

    response.flatMap { r =>
      val elapsed = stopwatch.elapsed().toMicros.toInt
      metrics.histogram("upstream_response_time",
                        elapsed,
                        "upstream" -> upstream.metricsTag)

      r.entity.withoutSizeLimit().toStrict(entityConsumptionTimeout).map { e =>
        upstream.transformResponse(preparedProxyRequest, r.withEntity(e))
      }
    }
  }
}
