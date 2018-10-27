package com.pagerduty.akka.http.proxy.filter

import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

object NoOpRequestFilter extends RequestFilter[Any] {
  def apply(request: HttpRequest, data: Option[Any]): RequestFilterOutput =
    Future.successful(Right(request))
}
