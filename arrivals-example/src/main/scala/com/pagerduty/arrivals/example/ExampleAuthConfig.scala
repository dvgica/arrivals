package com.pagerduty.arrivals.example

import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.model.headers.RawHeader
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthFailedReason
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig

import scala.concurrent.Future
import scala.util.{Success, Try}

class ExampleAuthConfig extends HeaderAuthConfig {
  type Cred = String
  type AuthData = UserId
  type Permission = Nothing

  def extractCredentials(request: HttpRequest)(implicit reqMeta: RequestMetadata): List[Cred] = {
    // obviously, this is a terrible, terrible idea that you should never do
    request.uri.query().get("username").toList
  }

  def authenticate(credential: Cred)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = {
    // for demo purposes.... don't do this
    Future.successful(Success(credential match {
      case "mittens" => Some(1)
      case "rex"     => Some(2)
      case _         => None
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
