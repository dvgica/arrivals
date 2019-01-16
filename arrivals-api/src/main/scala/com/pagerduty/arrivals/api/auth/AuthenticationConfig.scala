package com.pagerduty.arrivals.api.auth

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.akka.http.support.RequestMetadata

import scala.concurrent.Future
import scala.util.Try

/** Configuration for routes that authenticate a request. */
trait AuthenticationConfig {

  /** The type of authentication data expected to be returned upon the successful authentication of a request.
    *
    * This type typically holds additional information about an authenticated user, such as user ID, account ID, etc.
    */
  type AuthData

  /** The type of permission that may be required for the request to be considered "authenticated".
    *
    * Mixing authentication and authorization like this is not great, to say the least. This interface is likely to change.
    */
  type Permission

  /** Defines how a given request is authenticated.
    *
    * @param request A request to authenticate
    * @param reqMeta Request metadata, including request ID
    * @return Data about the authenticated user on success, None otherwise.
    */
  def authenticate(request: HttpRequest)(implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]]

  /** Defines if an authenticated user has the required permission to access a route.
    *
    * Again, this API is likely to change.
    *
    * @param authData Data associated with the authenticated user
    * @param request The incoming request
    * @param permission The required permission
    * @param reqMeta Request metadata, including request ID
    * @return `Some` reason the permission check failed, or `None` if it succeeded
    */
  def authDataGrantsPermission(
      authData: AuthData,
      request: HttpRequest,
      permission: Option[Permission]
    )(implicit reqMeta: RequestMetadata
    ): Option[AuthFailedReason]
}
