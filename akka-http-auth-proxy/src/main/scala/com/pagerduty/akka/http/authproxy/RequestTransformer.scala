package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.HttpRequest

import scala.concurrent.Future

trait RequestTransformer[AuthData] {
  def transformRequest(request: HttpRequest,
                       optAuthData: Option[AuthData]): Future[HttpRequest]
}
