package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

object NoOpRequestFilter extends RequestFilter[Any] {
  def apply(request: HttpRequest, data: Any): RequestFilterOutput =
    Future.successful(Right(request))
}
