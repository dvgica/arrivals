package com.pagerduty.akka.http.proxy.filter

import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

trait RequestFilter[-RequestData] {
  def apply(request: HttpRequest,
            data: Option[RequestData]): RequestFilterOutput
}
