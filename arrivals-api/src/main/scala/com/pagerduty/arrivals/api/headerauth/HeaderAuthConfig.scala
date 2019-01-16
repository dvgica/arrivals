package com.pagerduty.arrivals.api.headerauth

import akka.http.scaladsl.model.HttpHeader
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthenticationConfig

/** Configuration for routes that add an HTTP header to authenticated requests before proxying. */
trait HeaderAuthConfig extends AuthenticationConfig {

  /** Define a method returning the name of the header to add. */
  def authHeaderName: String

  /** Given `AuthData`, return the `HttpHeader` to add to the authenticated `HttpRequest`. */
  def dataToAuthHeader(data: AuthData)(implicit reqMeta: RequestMetadata): HttpHeader
}
