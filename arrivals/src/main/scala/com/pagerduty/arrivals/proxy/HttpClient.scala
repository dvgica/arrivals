package com.pagerduty.arrivals.proxy

import akka.http.scaladsl.model.ws.{Message, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.scaladsl.Flow

import scala.concurrent.Future

/** An interface for Akka's HTTP client, useful for unit testing. */
trait HttpClient {
  def executeRequest(request: HttpRequest): Future[HttpResponse]

  def executeWebSocketRequest[T](
      request: WebSocketRequest,
      clientFlow: Flow[Message, Message, T]
    ): (Future[WebSocketUpgradeResponse], T)
}
