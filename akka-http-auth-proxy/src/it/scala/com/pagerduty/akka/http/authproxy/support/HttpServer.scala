package com.pagerduty.akka.http.authproxy.support

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.pagerduty.metrics.Metrics
import org.slf4j.LoggerFactory
import akka.http.scaladsl.server.Directives.{handleExceptions, _}
import com.pagerduty.akka.http.authproxy.AuthProxyController
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.proxy.{
  ErrorHandling,
  HttpProxy,
  CommonHostnameUpstream
}
import com.pagerduty.akka.http.requestauthentication.RequestAuthenticator

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

  val incidentUpstream = new CommonHostnameUpstream {
    val port = servicePort
    val metricsTag = "test"
  }
  val httpRoutes = {

    (handleExceptions(proxyExceptionHandler)) {
      prefixProxyRoute("api" / "v1" / "incidents", incidentUpstream) ~
        prefixProxyRoute("api" / "v2" / "incidents",
                         incidentUpstream,
                         IncidentsPermission) ~
        prefixProxyRoute("api" / "v2" / "schedules",
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
