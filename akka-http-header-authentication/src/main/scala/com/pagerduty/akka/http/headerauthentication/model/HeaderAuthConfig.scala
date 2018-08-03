package com.pagerduty.akka.http.headerauthentication.model

import akka.http.scaladsl.model.HttpHeader
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationConfig
import com.pagerduty.akka.http.support.RequestMetadata

trait HeaderAuthConfig extends AuthenticationConfig {
  def authHeaderName: String
  def dataToAuthHeader(data: AuthData)(
      implicit reqMeta: RequestMetadata): HttpHeader
}
