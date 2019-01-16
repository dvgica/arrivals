package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

/** A [[ResponseFilter]] which always operates synchronously. */
trait SyncResponseFilter[-RequestData] extends ResponseFilter[RequestData] {
  override def apply(request: HttpRequest, response: HttpResponse, data: RequestData): Future[HttpResponse] =
    Future.successful(applySync(request, response, data))

  /** Define the synchronous filter in this method. */
  def applySync(request: HttpRequest, response: HttpResponse, data: RequestData): HttpResponse
}
