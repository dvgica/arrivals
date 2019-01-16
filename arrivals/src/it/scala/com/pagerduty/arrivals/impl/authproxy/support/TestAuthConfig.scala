package com.pagerduty.arrivals.impl.authproxy.support

import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken, RawHeader}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthFailedReason
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig

import scala.concurrent.Future
import scala.util.{Success, Try}

object TestAuthConfig {
  val authData = "authenticated-with-incidents-permission"
  val authHeader = RawHeader("X-Authenticated", "true")
}

sealed trait StringPermission {
  def value: String
}
case object IncidentsPermission extends StringPermission {
  val value = "incidents"
}
case object SchedulesPermission extends StringPermission {
  val value = "schedules"
}

class TestAuthConfig extends HeaderAuthConfig {
  import TestAuthConfig._

  type AuthData = String
  type Permission = StringPermission

  def authenticate(request: HttpRequest)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = {
    val token = request.header[Authorization].flatMap {
      case Authorization(OAuth2BearerToken(t)) => Some(t)
      case _                                   => None
    }

    token match {
      case Some("GOODTOKEN") => Future.successful(Success(Some(authData)))
      case Some("BADTOKEN")  => Future.successful(Success(None))
      case _                 => Future.successful(Success(None))
    }
  }

  def authDataGrantsPermission(
      authData: AuthData,
      request: HttpRequest,
      permission: Option[Permission]
    )(implicit reqMeta: RequestMetadata
    ): Option[AuthFailedReason] = {
    permission match {
      case Some(p) =>
        if (authData.contains(p.value)) None
        else Some(new AuthFailedReason { val metricTag = "test" })
      case None => None
    }
  }

  def dataToAuthHeader(data: AuthData)(implicit reqMeta: RequestMetadata): HttpHeader = authHeader
  def authHeaderName: String = authHeader.name
}
