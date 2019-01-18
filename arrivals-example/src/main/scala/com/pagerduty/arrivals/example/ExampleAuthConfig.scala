package com.pagerduty.arrivals.example

import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.model.headers.RawHeader
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthFailedReason
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig

import scala.concurrent.Future
import scala.util.{Success, Try}

class ExampleAuthConfig extends HeaderAuthConfig {
  type AuthData = UserId
  type Permission = Nothing

  def authenticate(request: HttpRequest)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = {
    // obviously, this method is a terrible, terrible idea that you should never do
    val username = request.uri.query().get("username")

    Future.successful(Success(username match {
      case Some("mittens") => Some(1)
      case Some("rex")     => Some(2)
      case _               => None
    }))
  }

  def authDataGrantsPermission(
      authData: AuthData,
      request: HttpRequest,
      permission: Option[Permission]
    )(implicit reqMeta: RequestMetadata
    ): Option[AuthFailedReason] = {
    None
  }

  val authHeaderName = "X-User-Id"
  def dataToAuthHeader(data: AuthData)(implicit reqMeta: RequestMetadata): HttpHeader = {
    // normally this header should be cryptographically signed or something
    RawHeader(authHeaderName, data.toString)
  }
}
