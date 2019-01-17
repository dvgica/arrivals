package com.pagerduty.arrivals.headerauth

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directive0
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import akka.http.scaladsl.server.Directives._
import com.pagerduty.akka.http.support.RequestMetadata

object AuthHeaderDirectives {

  def addAuthHeader(
      authConfig: HeaderAuthConfig
    )(optAuthData: Option[authConfig.AuthData]
    )(implicit reqMetadata: RequestMetadata
    ): Directive0 = {
    mapRequest { req =>
      optAuthData match {
        case Some(authData) => addAuthenticationHeader(authConfig)(req, authData)
        case None           => req
      }
    }
  }

  private def addAuthenticationHeader(
      authConfig: HeaderAuthConfig
    )(request: HttpRequest,
      authData: authConfig.AuthData
    )(implicit reqMeta: RequestMetadata
    ): HttpRequest = {
    val authHeader = authConfig.dataToAuthHeader(authData)
    request.addHeader(authHeader)
  }
}
