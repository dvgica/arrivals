package com.pagerduty.arrivals.api.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

trait ResponseFilter[-RequestData] {
  def apply(request: HttpRequest, response: HttpResponse, data: RequestData): ResponseFilterOutput
}
