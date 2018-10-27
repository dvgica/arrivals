package com.pagerduty.akka.http.proxy.filter

import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

trait SyncRequestFilter[-RequestData] extends RequestFilter[RequestData] {
  override def apply(request: HttpRequest,
                     data: Option[RequestData]): RequestFilterOutput =
    Future.successful(applySync(request, data))

  def applySync(request: HttpRequest,
                data: Option[RequestData]): SyncRequestFilterOutput
}
