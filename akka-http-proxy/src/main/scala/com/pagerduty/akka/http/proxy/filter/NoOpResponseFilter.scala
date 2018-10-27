package com.pagerduty.akka.http.proxy.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

object NoOpResponseFilter extends ResponseFilter[Any] {
  def apply(request: HttpRequest,
            response: HttpResponse,
            data: Option[Any]): ResponseFilterOutput =
    Future.successful(response)
}
