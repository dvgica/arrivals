package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

/** A [[RequestFilter]] which always operates synchronously. */
trait SyncRequestFilter[-RequestData] extends RequestFilter[RequestData] {
  override def apply(request: HttpRequest, data: RequestData): Future[Either[HttpResponse, HttpRequest]] =
    Future.successful(applySync(request, data))

  /** Define the synchronous filter in this method. */
  def applySync(request: HttpRequest, data: RequestData): Either[HttpResponse, HttpRequest]
}
