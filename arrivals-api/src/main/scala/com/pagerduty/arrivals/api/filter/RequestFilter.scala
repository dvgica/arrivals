package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

/** Filters a `HttpRequest` by either transforming it into a new one, or returning an `HttpResponse`.
  * If an `HttpResponse` is returned by the filter, it is immediately returned to the client, skipping any following
  * filters, aggregation, or proxying code. If an `HttpRequest` is returned, it will be passed to any subsequent filters,
  * aggregation, or proxying code.
  *
  * @tparam RequestData Type of data required to apply the filter. Use `Any` if no data is required.
  */
trait RequestFilter[-RequestData] {

  /** Define the filter in this method. */
  def apply(request: HttpRequest, data: RequestData): Future[Either[HttpResponse, HttpRequest]]
}
