package com.pagerduty.arrivals.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.impl.aggregator.{
  AggregatorController,
  AggregatorRequestHandler
}
import com.pagerduty.arrivals.impl.auth.{
  RequestAuthenticator,
  RequireAuthentication
}
import com.pagerduty.arrivals.impl.authproxy.{
  AuthProxyController,
  AuthProxyRequestHandler
}
import com.pagerduty.arrivals.impl.headerauth.HeaderAuthenticator
import com.pagerduty.arrivals.impl.proxy.{
  HttpProxy,
  ProxyController,
  ProxyRequestHandler
}
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
    entityConsumptionTimeout: FiniteDuration = 20.seconds)(
    implicit actorSystem: ActorSystem,
    val materializer: Materializer,
    metrics: Metrics = NullMetrics)
    extends ProxyController[AddressingConfig]
    with AuthProxyController[AuthConfig, AddressingConfig]
    with AggregatorController[AuthConfig, AddressingConfig] { outer =>

  def routes: Route

  val logger = LoggerFactory.getLogger(getClass)

  logger.info("Starting HttpServer...")
  val authConfig = headerAuthConfig
  implicit val executionContext = actorSystem.dispatcher

  val httpProxy =
    new HttpProxy(addressingConfig, Http().singleRequest(_))

  val proxyRequestHandler =
    new ProxyRequestHandler[AddressingConfig] {
      val executionContext = outer.executionContext
    }

  val authProxyRequestHandler =
    new AuthProxyRequestHandler[AddressingConfig, AuthConfig#AuthData] {
      val executionContext = outer.executionContext
    }

  val aggregatorRequestHandler =
    new AggregatorRequestHandler[AddressingConfig, AuthConfig#AuthData] {
      val executionContext = outer.executionContext
    }

  val requestAuthenticator = new RequestAuthenticator {
    val executionContext = outer.executionContext
    val metrics = outer.metrics
  }

  val requireAuthentication = new RequireAuthentication {
    val requestAuthenticator = outer.requestAuthenticator
  }

  val headerAuthenticator = new HeaderAuthenticator {
    val requestAuthenticator = outer.requestAuthenticator
  }

  logger.info(
    s"Akka-HTTP binding to port: $listenPort and interface: $listenInterface...")
  private val bindingFuture =
    Http(actorSystem).bindAndHandle(routes, listenInterface, listenPort)(
      materializer)

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
