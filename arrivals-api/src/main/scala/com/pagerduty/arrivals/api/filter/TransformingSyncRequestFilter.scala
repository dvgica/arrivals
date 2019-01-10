package com.pagerduty.arrivals.api.filter
import akka.http.scaladsl.model.HttpRequest

trait TransformingSyncRequestFilter[-RequestData] extends SyncRequestFilter[RequestData] {
  override def applySync(request: HttpRequest, data: RequestData): Right[Nothing, HttpRequest] =
    Right(applyTransformSync(request, data))

  def applyTransformSync(request: HttpRequest, data: RequestData): HttpRequest
}
