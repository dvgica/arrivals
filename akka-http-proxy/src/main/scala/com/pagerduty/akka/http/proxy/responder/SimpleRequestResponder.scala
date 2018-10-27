package com.pagerduty.akka.http.proxy.responder

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait SimpleRequestResponder[-RequestData] {
  def apply(request: HttpRequest,
            data: Option[RequestData]): Future[HttpResponse]
}
