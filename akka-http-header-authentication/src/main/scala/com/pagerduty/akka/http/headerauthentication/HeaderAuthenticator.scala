package com.pagerduty.akka.http.headerauthentication

import akka.http.scaladsl.model.StatusCodes.Unauthorized
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.requestauthentication.RequestAuthenticator
import com.pagerduty.akka.http.support.{MetadataLogging, RequestMetadata}

import scala.concurrent.Future

trait HeaderAuthenticator extends MetadataLogging {
  def requestAuthenticator: RequestAuthenticator

  def addAuthHeader(
      authConfig: HeaderAuthConfig
  )(request: HttpRequest,
    stripAuthorizationHeader: Boolean = true,
    requiredPermission: Option[authConfig.Permission] = None)(
      handler: (HttpRequest,
                Option[authConfig.AuthData]) => Future[HttpResponse])(
      implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
    requestAuthenticator.authenticate(authConfig)(request,
                                                  stripAuthorizationHeader,
                                                  requiredPermission) {
      (request, optAuthData) =>
        val possiblyAuthedRequest = optAuthData match {
          case Some(data) => addAuthenticationHeader(authConfig)(request, data)
          case None =>
            log.debug("Authentication failed, not adding auth header!")
            request
        }
        handler(possiblyAuthedRequest, optAuthData)
    }
  }

  def addAndRequireAuthHeader(
      authConfig: HeaderAuthConfig
  )(request: HttpRequest,
    stripAuthorizationHeader: Boolean = true,
    requiredPermission: Option[authConfig.Permission] = None)(
      handler: (HttpRequest, authConfig.AuthData) => Future[HttpResponse])(
      implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
    requestAuthenticator.authenticate(authConfig)(request,
                                                  stripAuthorizationHeader,
                                                  requiredPermission) {
      (request, optAuthData) =>
        optAuthData match {
          case Some(data) =>
            val authedRequest =
              addAuthenticationHeader(authConfig)(request, data)
            handler(authedRequest, data)
          case None =>
            Future.successful(HttpResponse(Unauthorized))
        }
    }
  }

  private def addAuthenticationHeader(
      authConfig: HeaderAuthConfig
  )(request: HttpRequest, authData: authConfig.AuthData)(
      implicit reqMeta: RequestMetadata): HttpRequest = {
    val authHeader = authConfig.dataToAuthHeader(authData)
    request.addHeader(authHeader)
  }
}
