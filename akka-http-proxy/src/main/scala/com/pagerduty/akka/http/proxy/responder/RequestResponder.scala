package com.pagerduty.akka.http.proxy.responder

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait RequestResponder[T, -RequestData] {
  def apply(request: HttpRequest,
            t: T,
            data: Option[RequestData]): Future[HttpResponse]
}
