package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.filter.{
  NoOpRequestFilter,
  NoOpResponseFilter,
  RequestFilter,
  ResponseFilter
}
import com.pagerduty.akka.http.proxy.{HttpProxy, RequestHandler, Upstream}
import com.pagerduty.akka.http.support.RequestMetadata

trait AuthProxyController[AuthConfig <: HeaderAuthConfig, AddressingConfig] {

  val authConfig: AuthConfig
  def httpProxy: HttpProxy[AddressingConfig]
  def proxyRequestHandler
    : RequestHandler[Upstream[AddressingConfig], AuthConfig#AuthData]
  def headerAuthenticator: HeaderAuthenticator

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData]
  ): Route =
    prefixAuthProxyRoute(path, upstream, None, requestFilter)

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
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
      requestFilter: RequestFilter[AuthConfig#AuthData]
  ): Route =
    prefixAuthProxyRoute(path,
                         upstream,
                         Option(requiredPermission),
                         requestFilter)

  def prefixAuthProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[AuthConfig#AuthData]
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
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
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
      requestFilter: RequestFilter[AuthConfig#AuthData] = NoOpRequestFilter,
      responseFilter: ResponseFilter[AuthConfig#AuthData] = NoOpResponseFilter
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
      requestFilter: RequestFilter[AuthConfig#AuthData]): Route =
    authProxyRoute(upstream, None, requestFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[AuthConfig#AuthData]): Route =
    authProxyRoute(upstream, None, NoOpRequestFilter, responseFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData]): Route =
    authProxyRoute(upstream, Some(requiredPermission), requestFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[AuthConfig#AuthData]): Route =
    authProxyRoute(upstream,
                   Some(requiredPermission),
                   NoOpRequestFilter,
                   responseFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
  ): Route =
    authProxyRoute(upstream, None, requestFilter, responseFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
  ): Route =
    authProxyRoute(upstream,
                   Option(requiredPermission),
                   requestFilter,
                   responseFilter)

  def authProxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[AuthConfig#AuthData] = NoOpRequestFilter,
      responseFilter: ResponseFilter[AuthConfig#AuthData] = NoOpResponseFilter
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
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
  )(implicit reqMeta: RequestMetadata): Route = {
    complete {
      headerAuthenticator.addAuthHeader(authConfig)(
        request,
        requiredPermission.asInstanceOf[Option[authConfig.Permission]]
      ) {
        case (authedRequest, optAuthData) =>
          proxyRequestHandler.handle(authedRequest,
                                     upstream,
                                     httpProxy,
                                     requestFilter,
                                     responseFilter)
      }
    }
  }

}
