package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

object NoOpResponseFilter extends ResponseFilter[Any] {
  def apply(request: HttpRequest, response: HttpResponse, data: Any): ResponseFilterOutput =
    Future.successful(response)
}
