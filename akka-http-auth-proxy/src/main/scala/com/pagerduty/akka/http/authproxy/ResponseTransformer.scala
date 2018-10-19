package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig

import scala.concurrent.Future

trait ResponseTransformer[AuthData] {
  def transformResponse(request: HttpRequest,
                        response: HttpResponse,
                        optAuthData: Option[AuthData]): Future[HttpResponse]
}
