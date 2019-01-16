package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

/** Filters a `HttpResponse` by transforming it into a new `HttpResponse`.
  *
  * Unlike [[RequestFilter]], there is no way to short-circuit the execution of following filters.
  *
  * @tparam RequestData Type of data required to apply the filter. Use `Any` if no data is required.
  */
trait ResponseFilter[-RequestData] {

  /** Define the filter in this method. */
  def apply(request: HttpRequest, response: HttpResponse, data: RequestData): Future[HttpResponse]
}
