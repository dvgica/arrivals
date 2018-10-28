package com.pagerduty.arrivals.impl.authproxy.support

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Authority
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.stream.ActorMaterializer
import com.pagerduty.metrics.Metrics
import org.slf4j.LoggerFactory
import akka.http.scaladsl.server.Directives.{handleExceptions, _}
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.impl.auth.RequestAuthenticator
import com.pagerduty.arrivals.impl.authproxy.{
  AuthProxyController,
  AuthProxyRequestHandler
}
import com.pagerduty.arrivals.impl.headerauth.HeaderAuthenticator
import com.pagerduty.arrivals.impl.proxy.{ErrorHandling, HttpProxy}

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.TimeoutException
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

class HttpServer(
    val httpInterface: String,
    val port: Int,
    val servicePort: Int,
    val httpProxy: HttpProxy[String]
)(implicit actorSystem: ActorSystem,
  materializer: ActorMaterializer,
  val metrics: Metrics)
    extends AuthProxyController[TestAuthConfig, String]
    with ErrorHandling { outer =>

  val log = LoggerFactory.getLogger(getClass)

  val executionContext = actorSystem.dispatcher

  val authConfig = new TestAuthConfig

  val requestAuthenticator = new RequestAuthenticator {
    val executionContext = outer.executionContext
    val metrics = outer.metrics
  }

  val headerAuthenticator = new HeaderAuthenticator {
    val requestAuthenticator = outer.requestAuthenticator
  }

  val incidentUpstream = new Upstream[String] {
    def addressRequest(request: HttpRequest,
                       addressingConfig: String): HttpRequest = {
      val uri = request.uri.withAuthority(
        Authority(Uri.Host("localhost"), servicePort))
      request.withUri(uri)
    }
    val metricsTag = "test"
  }

  val authProxyRequestHandler =
    new AuthProxyRequestHandler[String, TestAuthConfig#AuthData] {
      val executionContext = outer.executionContext
    }
  val httpRoutes = {

    (handleExceptions(proxyExceptionHandler)) {
      prefixAuthProxyRoute("api" / "v1" / "incidents", incidentUpstream) ~
        prefixAuthProxyRoute("api" / "v2" / "incidents",
                             incidentUpstream,
                             IncidentsPermission) ~
        prefixAuthProxyRoute("api" / "v2" / "schedules",
                             incidentUpstream,
                             SchedulesPermission)
    }
  }

  log.info(
    s"Akka-HTTP binding to port: $port and interface: $httpInterface...")
  private val bindingFuture =
    Http(actorSystem).bindAndHandle(httpRoutes, httpInterface, port)(
      materializer)

  // for simplicity, block until the HTTP server is actually started
  private val tryBinding = Try(Await.result(bindingFuture, 10.seconds))

  private val binding = tryBinding match {
    case Success(b) =>
      log.info(
        s"Successfully bound to port: ${b.localAddress.getPort} on interface: $httpInterface")
      log.info("HttpServer started")
      b
    case Failure(e) =>
      log.info(s"Failed to bind to port, exception: $e")
      throw e
  }

  def stop(): Unit = {
    log.info("Stopping HttpServer gracefully...")
    Await.ready(binding.unbind(), 10.seconds)
    log.info(
      "HttpServer unbound, waiting 3 seconds to try to complete pending requests...")
    try {
      Await.ready(Future.never, 3.seconds)
    } catch {
      case _: InterruptedException | _: TimeoutException =>
    }
    log.info("HttpServer stopped")
  }
}
