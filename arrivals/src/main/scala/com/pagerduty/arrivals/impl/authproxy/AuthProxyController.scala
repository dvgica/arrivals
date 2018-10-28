package com.pagerduty.arrivals.impl.authproxy

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.arrivals.api.filter.{
  NoOpRequestFilter,
  NoOpResponseFilter,
  RequestFilter,
  ResponseFilter
}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.api.proxy.{HttpProxy, Upstream}
import com.pagerduty.arrivals.impl.headerauth.HeaderAuthenticator

trait AuthProxyController[AuthConfig <: HeaderAuthConfig, AddressingConfig] {

  val authConfig: AuthConfig
  def httpProxy: HttpProxy[AddressingConfig]
  def authProxyRequestHandler
    : AuthProxyRequestHandler[AddressingConfig, AuthConfig#AuthData]
  def headerAuthenticator: HeaderAuthenticator

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]]
  ): Route =
    prefixAuthProxyRoute(path, upstream, None, requestFilter)

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
  ): Route =
    prefixAuthProxyRoute(path, upstream, None, responseFilter = responseFilter)

  def prefixAuthProxyRoute(path: PathMatcher[Unit],
                           upstream: Upstream[AddressingConfig],
                           requiredPermission: AuthConfig#Permission): Route =
    prefixAuthProxyRoute(path, upstream, Option(requiredPermission))

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]]
  ): Route =
    prefixAuthProxyRoute(path,
                         upstream,
                         Option(requiredPermission),
                         requestFilter)

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
  ): Route =
    prefixAuthProxyRoute(path,
                         upstream,
                         Option(requiredPermission),
                         NoOpRequestFilter,
                         responseFilter)

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
  ): Route =
    prefixAuthProxyRoute(path,
                         upstream,
                         Option(requiredPermission),
                         requestFilter,
                         responseFilter)

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]] =
        NoOpRequestFilter,
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]] =
        NoOpResponseFilter
  ): Route =
    pathPrefix(path) {
      authProxyRoute(upstream,
                     requiredPermission,
                     requestFilter,
                     responseFilter)
    }

  def authProxyRoute(upstream: Upstream[AddressingConfig],
                     requiredPermission: AuthConfig#Permission): Route =
    authProxyRoute(upstream, Some(requiredPermission))

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]]): Route =
    authProxyRoute(upstream, None, requestFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]): Route =
    authProxyRoute(upstream, None, NoOpRequestFilter, responseFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]]): Route =
    authProxyRoute(upstream, Some(requiredPermission), requestFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]): Route =
    authProxyRoute(upstream,
                   Some(requiredPermission),
                   NoOpRequestFilter,
                   responseFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
  ): Route =
    authProxyRoute(upstream, None, requestFilter, responseFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
  ): Route =
    authProxyRoute(upstream,
                   Option(requiredPermission),
                   requestFilter,
                   responseFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]] =
        NoOpRequestFilter,
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]] =
        NoOpResponseFilter
  ): Route =
    extractRequest { request =>
      implicit val reqMeta = RequestMetadata.fromRequest(request)

      authenticateAndProxyRequest(
        upstream,
        request,
        requiredPermission,
        requestFilter,
        responseFilter
      )
    }

  private def authenticateAndProxyRequest(
      upstream: Upstream[AddressingConfig],
      request: HttpRequest,
      requiredPermission: Option[AuthConfig#Permission],
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
  )(implicit reqMeta: RequestMetadata): Route = {
    complete {
      headerAuthenticator.addAuthHeader(authConfig)(
        request,
        requiredPermission.asInstanceOf[Option[authConfig.Permission]]
      ) {
        case (authedRequest, optAuthData) =>
          authProxyRequestHandler.apply(authedRequest,
                                        upstream,
                                        optAuthData,
                                        httpProxy,
                                        requestFilter,
                                        responseFilter)
      }
    }
  }

}
