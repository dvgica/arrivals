package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait ResponseFilter[-RequestData] {
  def apply(request: HttpRequest, response: HttpResponse, data: RequestData): Future[HttpResponse]
}
