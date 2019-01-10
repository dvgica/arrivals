package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait RequestFilter[-RequestData] {
  def apply(request: HttpRequest, data: RequestData): Future[Either[HttpResponse, HttpRequest]]
}
