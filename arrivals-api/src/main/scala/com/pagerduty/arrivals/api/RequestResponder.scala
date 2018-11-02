package com.pagerduty.arrivals.api

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait RequestResponder[-Input, -RequestData] {
  def apply(request: HttpRequest, input: Input, data: RequestData): Future[HttpResponse]
}
