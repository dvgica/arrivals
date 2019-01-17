package com.pagerduty.arrivals

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.pagerduty.arrivals.proxy.{HttpClient, HttpProxy}
import com.pagerduty.metrics.{Metrics, NullMetrics}

import scala.concurrent.Future
import scala.concurrent.duration.{FiniteDuration, _}

object ArrivalsContext {
  def buildHttpClient(http: HttpExt, materializer: Materializer): HttpClient = {
    new HttpClient {
      override def executeRequest(request: HttpRequest) = http.singleRequest(request)
      override def executeWebSocketRequest[T](request: WebSocketRequest, clientFlow: Flow[Message, Message, T]) =
        http.singleWebSocketRequest(request, clientFlow)(materializer)
    }
  }
}

/** A dependency implicitly required by all Arrivals routes.
  *
  * @param addressingConfig Configuration used to address a request for an [[com.pagerduty.arrivals.api.proxy.Upstream]]
  * @param entityConsumptionTimeout Maximum time to wait while consuming an upstream response
  * @param buildHttpClient A factory function for building the HTTP client used in the proxy. Useful for unit testing.
  * @param actorSystem An Akka Actor system.
  * @param materializer An Akka Streams Materializer
  * @param metrics A metrics provider
  * @tparam AddressingConfig
  */
case class ArrivalsContext[AddressingConfig](
    addressingConfig: AddressingConfig,
    entityConsumptionTimeout: FiniteDuration = 20.seconds,
    buildHttpClient: (HttpExt, Materializer) => HttpClient = ArrivalsContext.buildHttpClient
  )(implicit actorSystem: ActorSystem,
    val materializer: Materializer,
    val metrics: Metrics = NullMetrics) {

  implicit val executionContext = actorSystem.dispatcher

  val http = Http()

  val httpClient = buildHttpClient(http, materializer)

  val httpProxy = new HttpProxy(addressingConfig, httpClient)

  def shutdown(): Future[Unit] = {
    http.shutdownAllConnectionPools()
  }
}
