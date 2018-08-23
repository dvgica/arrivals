package com.pagerduty.akka.http.aggregator.support

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationData.AuthFailedReason
import com.pagerduty.akka.http.support.RequestMetadata

import scala.concurrent.Future
import scala.util.Try

class TestAuthConfig extends HeaderAuthConfig {
  type Cred = String
  type AuthData = String
  type Permission = String
  type AuthHeader = RawHeader

  def extractCredentials(request: HttpRequest)(
      implicit reqMeta: RequestMetadata): List[Cred] = ???

  def authenticate(credential: Cred)(
      implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = ???

  def authDataGrantsPermission(
      authData: AuthData,
      request: HttpRequest,
      permission: Option[Permission]
  )(implicit reqMeta: RequestMetadata): Option[AuthFailedReason] = ???

  def dataToAuthHeader(data: AuthData)(
      implicit reqMeta: RequestMetadata): AuthHeader = ???
  def authHeaderName: String = ???
}
