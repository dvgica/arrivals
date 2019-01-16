package com.pagerduty.arrivals.api.filter
import akka.http.scaladsl.model.HttpRequest

/** A [[RequestFilter]] which always transforms an `HttpRequest` synchronously. */
trait TransformingSyncRequestFilter[-RequestData] extends SyncRequestFilter[RequestData] {
  override def applySync(request: HttpRequest, data: RequestData): Right[Nothing, HttpRequest] =
    Right(applyTransformSync(request, data))

  /** Define the synchronous transform in this method. */
  def applyTransformSync(request: HttpRequest, data: RequestData): HttpRequest
}
