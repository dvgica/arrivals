package com.pagerduty.arrivals.api.headerauth

import akka.http.scaladsl.model.HttpHeader
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthenticationConfig

trait HeaderAuthConfig extends AuthenticationConfig {
  def authHeaderName: String
  def dataToAuthHeader(data: AuthData)(implicit reqMeta: RequestMetadata): HttpHeader
}
