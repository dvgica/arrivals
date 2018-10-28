package com.pagerduty.arrivals.impl.auth

import akka.http.scaladsl.model.StatusCodes.Unauthorized
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.auth.AuthenticationConfig

import scala.concurrent.Future

trait RequireAuthentication {
  def requestAuthenticator: RequestAuthenticator

  def apply(
      authConfig: AuthenticationConfig
  )(request: HttpRequest, requiredPermission: Option[authConfig.Permission])(
      handler: (HttpRequest, authConfig.AuthData) => Future[HttpResponse])(
      implicit reqMeta: RequestMetadata): Future[HttpResponse] = {
    requestAuthenticator.authenticate(authConfig)(request, requiredPermission) {
      (req: HttpRequest, optAuthData: Option[authConfig.AuthData]) =>
        optAuthData match {
          case Some(authData) => handler(req, authData)
          case None => Future.successful(HttpResponse(Unauthorized))
        }
    }
  }
}
