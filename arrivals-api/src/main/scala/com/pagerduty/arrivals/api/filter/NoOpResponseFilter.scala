package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

/** A [[ResponseFilter]] that passes through the request it is given, unchanged. */
object NoOpResponseFilter extends ResponseFilter[Any] {
  def apply(request: HttpRequest, response: HttpResponse, data: Any): Future[HttpResponse] =
    Future.successful(response)
}
