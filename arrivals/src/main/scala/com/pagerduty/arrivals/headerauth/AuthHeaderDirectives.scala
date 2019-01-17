package com.pagerduty.arrivals.headerauth

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directive0
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import akka.http.scaladsl.server.Directives._
import com.pagerduty.akka.http.support.RequestMetadata

/** These directives add a header to authenticated requests.
  *
  * This directive is useful for proving to [[com.pagerduty.arrivals.api.proxy.Upstream]]s that authentication has successfully
  * taken place. However, since an attacker may attempt to forge this header, it should likely be cryptographically signed
  * and verified by the `Upstream`. That implementation is out of scope for this library.
  */
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
