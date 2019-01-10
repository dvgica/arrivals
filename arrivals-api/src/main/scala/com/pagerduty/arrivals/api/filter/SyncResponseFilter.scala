package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait SyncResponseFilter[-RequestData] extends ResponseFilter[RequestData] {
  override def apply(request: HttpRequest, response: HttpResponse, data: RequestData): Future[HttpResponse] =
    Future.successful(applySync(request, response, data))

  def applySync(request: HttpRequest, response: HttpResponse, data: RequestData): HttpResponse
}
