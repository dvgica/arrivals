package com.pagerduty.akka.http.proxy.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait ResponseFilter[-RequestData] {
  def apply(request: HttpRequest,
            response: HttpResponse,
            data: Option[RequestData]): ResponseFilterOutput
}
