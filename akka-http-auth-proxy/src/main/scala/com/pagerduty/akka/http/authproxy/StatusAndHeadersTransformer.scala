package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.HttpResponse

import scala.concurrent.Future

trait StatusAndHeadersTransformer[AuthData]
    extends ResponseTransformer[AuthData] {
  override def transformResponse(
      response: HttpResponse,
      optAuthData: Option[AuthData]): Future[HttpResponse] = {
    Future.successful(transformStatusAndHeaders(response, optAuthData))
  }

  def transformStatusAndHeaders(response: HttpResponse,
                                optAuthData: Option[AuthData]): HttpResponse
}
