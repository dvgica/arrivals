package com.pagerduty.arrivals.authproxy.support
import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.scaladsl.Flow
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.pagerduty.arrivals.proxy.{HttpClient, HttpProxy}
import com.pagerduty.metrics.NullMetrics
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

trait IntegrationSpec extends AnyFreeSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  val host = "localhost"
  val port = 1234

  val asHost = "localhost"
  val asPort = 2345

  val serviceHost = "localhost"
  val servicePort = 3456

  implicit var as: ActorSystem = _
  implicit var ec: ExecutionContext = _
  var http: HttpExt = _
  var s: HttpServer = _
  var mockService: WireMockServer = _
  var mockAs: WireMockServer = _

  override def beforeAll(): Unit = {
    as = ActorSystem("bff-public-api-test")
    ec = as.dispatcher
    implicit val metrics = NullMetrics
    http = Http()

    val httpClient = new HttpClient {
      override def executeRequest(request: HttpRequest) = http.singleRequest(request)
      override def executeWebSocketRequest[T](request: WebSocketRequest, clientFlow: Flow[Message, Message, T]) =
        http.singleWebSocketRequest(request, clientFlow)
    }

    val httpProxy = new HttpProxy("localhost", httpClient)
    s = new HttpServer(host, port, servicePort, 10100, httpProxy)

    mockService = new WireMockServer(options().port(servicePort))
    mockService.start()

    mockAs = new WireMockServer(options().port(asPort))
    mockAs.start()
  }

  override def beforeEach(): Unit = {
    mockService.resetAll()
    mockAs.resetAll()
  }

  def url(path: String): String = {
    s"http://$host:$port$path"
  }

  override def afterAll(): Unit = {
    cleanup()
  }

  def cleanup(): Unit = {
    http.shutdownAllConnectionPools()
    s.stop()
    mockService.stop()
    mockAs.stop()
    Await.ready(as.terminate(), Duration.Inf)
    ()
  }
}
