package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait SyncRequestFilter[-RequestData] extends RequestFilter[RequestData] {
  override def apply(request: HttpRequest, data: RequestData): Future[Either[HttpResponse, HttpRequest]] =
    Future.successful(applySync(request, data))

  def applySync(request: HttpRequest, data: RequestData): Either[HttpResponse, HttpRequest]
}
