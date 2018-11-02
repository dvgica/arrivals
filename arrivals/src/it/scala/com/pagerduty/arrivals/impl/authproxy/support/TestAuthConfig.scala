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

  type Cred = OAuth2BearerToken
  type AuthData = String
  type Permission = StringPermission

  def extractCredentials(request: HttpRequest)(implicit reqMeta: RequestMetadata): List[Cred] = {
    request.headers.flatMap {
      case Authorization(token: OAuth2BearerToken) => List(token)
      case _                                       => List()
    }.toList
  }

  def authenticate(credential: Cred)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = {
    credential.token match {
      case "GOODTOKEN" => Future.successful(Success(Some(authData)))
      case "BADTOKEN"  => Future.successful(Success(None))
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
        else Some(new AuthFailedReason("test"))
      case None => None
    }
  }

  def dataToAuthHeader(data: AuthData)(implicit reqMeta: RequestMetadata): HttpHeader = authHeader
  def authHeaderName: String = authHeader.name
}
