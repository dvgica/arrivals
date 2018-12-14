package com.pagerduty.arrivals.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.api.proxy.HttpClient
import com.pagerduty.arrivals.impl.aggregator.AggregatorController
import com.pagerduty.arrivals.impl.authproxy.AuthProxyController
import com.pagerduty.arrivals.impl.proxy.{HttpProxy, ProxyController}
import com.pagerduty.metrics.{Metrics, NullMetrics}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

abstract class ArrivalsServer[AddressingConfig, AuthConfig <: HeaderAuthConfig](
    addressingConfig: AddressingConfig,
    headerAuthConfig: AuthConfig,
    listenInterface: String = "0.0.0.0",
    listenPort: Int = 8080,
    entityConsumptionTimeout: FiniteDuration = 20.seconds
  )(implicit actorSystem: ActorSystem,
    val materializer: Materializer,
    metrics: Metrics = NullMetrics)
    extends ProxyController[AddressingConfig]
    with AuthProxyController[AuthConfig, AddressingConfig]
    with AggregatorController[AuthConfig, AddressingConfig] {

  def routes: Route

  val logger = LoggerFactory.getLogger(getClass)

  logger.info("Starting HttpServer...")
  val authConfig = headerAuthConfig
  implicit val executionContext = actorSystem.dispatcher

  val http = Http()

  val httpClient = new HttpClient {
    override def executeRequest(request: HttpRequest) = http.singleRequest(request)
    override def executeWebSocketRequest[T](request: WebSocketRequest, clientFlow: Flow[Message, Message, T]) =
      http.singleWebSocketRequest(request, clientFlow)
  }

  val httpProxy =
    new HttpProxy(addressingConfig, httpClient)

  logger.info(s"Akka-HTTP binding to port: $listenPort and interface: $listenInterface...")
  private val bindingFuture =
    Http(actorSystem).bindAndHandle(routes, listenInterface, listenPort)(materializer)

  //   for simplicity, block until the HTTP server is actually started
  private val tryBinding = Try(Await.result(bindingFuture, 10.seconds))

  private val binding = tryBinding match {
    case Success(b) =>
      logger.info(s"Successfully bound to ${b.localAddress}")
      logger.info("ArrivalsServer started")
      b
    case Failure(e) =>
      logger.info(s"Failed to bind to port, exception: $e")
      throw e
  }

  def stop(): Unit = {
    logger.info("Stopping ArrivalsServer gracefully...")
    Await.ready(binding.unbind(), 10.seconds)
    logger.info("ArrivalsServer stopped")
  }
}
