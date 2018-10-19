package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait SyncResponseTransformer[AuthData] extends ResponseTransformer[AuthData] {
  override def transformResponse(
      request: HttpRequest,
      response: HttpResponse,
      optAuthData: Option[AuthData]): Future[HttpResponse] = {
    Future.successful(transformResponseSync(response, optAuthData))
  }

  def transformResponseSync(response: HttpResponse,
                            optAuthData: Option[AuthData]): HttpResponse
}
