package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.pagerduty.akka.http.support.{MetadataLogging, RequestMetadata}
import com.pagerduty.metrics.{Metrics, Stopwatch}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object HttpProxy {
  val TimeoutAccessHeaderName = "Timeout-Access"

  val WebSocketHeadersToFilter = Set(
    "Sec-WebSocket-Accept",
    "Sec-WebSocket-Version",
    "Sec-WebSocket-Key",
    "Sec-WebSocket-Extensions",
    "UpgradeToWebSocket",
    "Upgrade"
  ).map(_.toLowerCase)
}

class HttpProxy[AddressingConfig](
    addressingConfig: AddressingConfig,
    httpClient: HttpClient,
    entityConsumptionTimeout: FiniteDuration = 20.seconds
)(implicit ec: ExecutionContext, materializer: Materializer, metrics: Metrics)
    extends MetadataLogging {
  import HttpProxy._

  def request(request: HttpRequest, upstream: Upstream[AddressingConfig])(
      implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
    val addressedRequest =
      upstream
        .addressRequestWithOverrides(request, addressingConfig)
        .removeHeader(TimeoutAccessHeaderName) // Akka HTTP server adds the Timeout-Access for internal reasons, but it should not be proxied

    val preparedProxyRequest =
      upstream.prepareRequestForDelivery(addressedRequest)

    val stopwatch = Stopwatch.start()

    val response = preparedProxyRequest.header[UpgradeToWebSocket] match {
      case Some(upgrade) =>
        proxyWebSocketRequest(preparedProxyRequest, upgrade)
      case None =>
        proxyHttpRequest(preparedProxyRequest)
    }

    response.map { r =>
      val elapsed = stopwatch.elapsed().toMicros.toInt
      metrics.histogram("upstream_response_time",
                        elapsed,
                        "upstream" -> upstream.metricsTag)
      r
    }
  }

  private def proxyHttpRequest(request: HttpRequest): Future[HttpResponse] = {
    val response = httpClient.executeRequest(request)
    response.flatMap { r =>
      r.entity.withoutSizeLimit().toStrict(entityConsumptionTimeout).map { e =>
        r.withEntity(e)
      }
    }
  }

  private def proxyWebSocketRequest(request: HttpRequest,
                                    upgrade: UpgradeToWebSocket)(
      implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
    // this code heavily inspired by https://github.com/DataBiosphere/leonardo/blob/develop/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/service/ProxyService.scala
    val flow =
      Flow.fromSinkAndSourceMat(Sink.asPublisher[Message](fanout = false),
                                Source.asSubscriber[Message])(Keep.both)

    val (responseFuture, (publisher, subscriber)) =
      httpClient.executeWebSocketRequest(
        WebSocketRequest(
          request.uri.withScheme("ws"),
          extraHeaders = filterWebSocketHeaders(request.headers),
          upgrade.requestedProtocols.headOption),
        flow
      )

    responseFuture.map {
      case ValidUpgrade(response, chosenSubprotocol) =>
        val webSocketResponse = upgrade.handleMessages(
          Flow.fromSinkAndSource(Sink.fromSubscriber(subscriber),
                                 Source.fromPublisher(publisher)),
          chosenSubprotocol
        )
        webSocketResponse.withHeaders(
          webSocketResponse.headers ++ filterWebSocketHeaders(
            response.headers))

      case InvalidUpgradeResponse(response, cause) =>
        log.warn(s"WebSocket upgrade response was invalid: $cause")
        response
    }
  }

  private def filterWebSocketHeaders(
      headers: immutable.Seq[HttpHeader]): immutable.Seq[HttpHeader] = {
    headers.filterNot(header =>
      WebSocketHeadersToFilter.contains(header.lowercaseName()))
  }
}
