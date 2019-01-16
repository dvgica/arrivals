package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

/** A [[RequestFilter]] that passes through the request it is given, unchanged. */
object NoOpRequestFilter extends RequestFilter[Any] {
  def apply(request: HttpRequest, data: Any): Future[Either[Nothing, HttpRequest]] =
    Future.successful(Right(request))
}
