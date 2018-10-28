package com.pagerduty.arrivals.api.auth

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.akka.http.support.RequestMetadata

import scala.concurrent.Future
import scala.util.Try

trait AuthenticationConfig {
  type Cred
  type AuthData
  type Permission

  def extractCredentials(request: HttpRequest)(
      implicit reqMeta: RequestMetadata): List[Cred]

  def authenticate(credential: Cred)(
      implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]]

  def authDataGrantsPermission(
      authData: AuthData,
      request: HttpRequest,
      permission: Option[Permission]
  )(implicit reqMeta: RequestMetadata): Option[AuthFailedReason]
}
