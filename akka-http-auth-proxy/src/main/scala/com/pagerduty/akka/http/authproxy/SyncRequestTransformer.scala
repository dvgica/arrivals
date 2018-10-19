package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig

import scala.concurrent.Future

trait SyncRequestTransformer[AuthData] extends RequestTransformer[AuthData] {
  override def transformRequest(
      request: HttpRequest,
      optAuthData: Option[AuthData]): Future[HttpRequest] = {
    Future.successful(transformRequestSync(request, optAuthData))
  }

  def transformRequestSync(request: HttpRequest,
                           optAuthData: Option[AuthData]): HttpRequest
}
