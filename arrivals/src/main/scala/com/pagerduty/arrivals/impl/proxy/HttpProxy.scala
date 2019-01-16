package com.pagerduty.arrivals.impl.proxy

import akka.http.scaladsl.model.ws._
import akka.http.scaladsl.model.{HttpHeader, HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.pagerduty.akka.http.support.{MetadataLogging, RequestMetadata}
import com.pagerduty.arrivals.api.proxy.Upstream
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
    "Upgrade",
    "Connection"
  ).map(_.toLowerCase)
}

class HttpProxy[AddressingConfig](
    addressingConfig: AddressingConfig,
    httpClient: HttpClient,
    entityConsumptionTimeout: FiniteDuration = 20.seconds
  )(implicit ec: ExecutionContext,
    materializer: Materializer,
    metrics: Metrics)
    extends HttpProxyLike[AddressingConfig]
    with MetadataLogging {
  import HttpProxy._

  def apply(request: HttpRequest, upstream: Upstream[AddressingConfig]): Future[HttpResponse] = {
    implicit val reqMeta = RequestMetadata.fromRequest(request)

    val addressedRequest =
      upstream
        .addressRequest(request, addressingConfig)
        .removeHeader(TimeoutAccessHeaderName) // Akka HTTP server adds the Timeout-Access for internal reasons, but it should not be proxied

    val upstreamFilterResult =
      upstream.requestFilter(addressedRequest, ())

    upstreamFilterResult.flatMap {
      case Right(filteredRequest) =>
        val stopwatch = Stopwatch.start()

        val response = filteredRequest.header[UpgradeToWebSocket] match {
          case Some(upgrade) =>
            proxyWebSocketRequest(filteredRequest, upgrade, upstream)
          case None =>
            proxyHttpRequest(filteredRequest, upstream)
        }

        response.map { r =>
          emitUpstreamResponseMetrics(r, stopwatch, upstream)
          r
        }
      case Left(response) => Future.successful(response)
    }
  }

  private def proxyHttpRequest(request: HttpRequest, upstream: Upstream[AddressingConfig]): Future[HttpResponse] = {
    val response = httpClient.executeRequest(request)
    response.flatMap { r =>
      r.entity.withoutSizeLimit().toStrict(entityConsumptionTimeout).flatMap { e =>
        upstream.responseFilter(request, r.withEntity(e), ())
      }
    }
  }

  private def proxyWebSocketRequest(
      request: HttpRequest,
      upgrade: UpgradeToWebSocket,
      upstream: Upstream[AddressingConfig]
    )(implicit reqMeta: RequestMetadata
    ): Future[HttpResponse] = {
    // this code heavily inspired by https://github.com/DataBiosphere/leonardo/blob/develop/src/main/scala/org/broadinstitute/dsde/workbench/leonardo/service/ProxyService.scala
    val flow =
      Flow.fromSinkAndSourceMat(Sink.asPublisher[Message](fanout = false), Source.asSubscriber[Message])(Keep.both)

    val (responseFuture, (publisher, subscriber)) =
      httpClient.executeWebSocketRequest(
        WebSocketRequest(
          request.uri.withScheme("ws"),
          extraHeaders = filterWebSocketHeaders(request.headers),
          upgrade.requestedProtocols.headOption
        ),
        flow
      )

    val response = responseFuture.map {
      case ValidUpgrade(response, chosenSubprotocol) =>
        val webSocketResponse = upgrade.handleMessages(
          Flow.fromSinkAndSource(Sink.fromSubscriber(subscriber), Source.fromPublisher(publisher)),
          chosenSubprotocol
        )
        webSocketResponse.withHeaders(webSocketResponse.headers ++ filterWebSocketHeaders(response.headers))

      case InvalidUpgradeResponse(response, cause) =>
        log.warn(s"WebSocket upgrade response was invalid: $cause")
        response
    }

    response.flatMap { resp =>
      upstream.responseFilter(request, resp, ())
    }
  }

  private def filterWebSocketHeaders(headers: immutable.Seq[HttpHeader]): immutable.Seq[HttpHeader] = {
    headers.filterNot(header => WebSocketHeadersToFilter.contains(header.lowercaseName()))
  }

  private def emitUpstreamResponseMetrics(
      response: HttpResponse,
      stopwatch: Stopwatch,
      upstream: Upstream[AddressingConfig]
    ): Unit = {
    val statusCode = response.status.intValue
    val responseErrorType = statusCode match {
      case i if i >= 400 && i <= 499 => "client"
      case i if i >= 500 && i <= 599 => "server"
      case _                         => "none"
    }

    val upstreamTag = "upstream" -> upstream.metricsTag
    val errorTags = Seq(
      ("response_code", statusCode.toString),
      ("response_error_type", responseErrorType)
    )

    val elapsed = stopwatch.elapsed().toMicros.toInt
    metrics.increment("upstream_response_count", (errorTags :+ upstreamTag): _*)
    metrics.histogram("upstream_response_time", elapsed, upstreamTag)
  }
}
