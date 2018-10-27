package com.pagerduty.akka.http.proxy.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait SyncResponseFilter[-RequestData] extends ResponseFilter[RequestData] {
  override def apply(request: HttpRequest,
                     response: HttpResponse,
                     data: Option[RequestData]): ResponseFilterOutput =
    Future.successful(applySync(request, response, data))

  def applySync(request: HttpRequest,
                response: HttpResponse,
                data: Option[RequestData]): SyncResponseFilterOutput
}
