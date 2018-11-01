package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.HttpRequest

trait RequestFilter[-RequestData] {
  def apply(request: HttpRequest, data: RequestData): RequestFilterOutput
}
