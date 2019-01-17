package com.pagerduty.arrivals.authproxy

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.ArrivalsContext
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.auth.AuthenticationDirectives._
import com.pagerduty.arrivals.headerauth.AuthHeaderDirectives._
import com.pagerduty.arrivals.filter.FilterDirectives._
import com.pagerduty.arrivals.proxy.ProxyRoutes.proxyRoute

/** Routes completed by proxying the request to an `Upstream`. If the request is authenticated, the authentication
  * header is added. **All requests are proxied regardless of authentication**.
  *
  * There are essentially two variants to the methods here:
  * - `prefixAuthProxyRoute` proxies all requests that have a path starting with the given `path`
  * - `authProxyRoute` proxies all requests
  *
  * If a `RequestFilter` is provided, it is run after authentication. Thus, `RequestFilter`s provided to these routes
  * must accept `Option[headerAuthConfig.AuthData]` as its `RequestData`.
  *
  * If a `ResponseFilter` is provided, it is run on the proxied response. It has a similar restriction on `RequestData`.
  *
  * @param headerAuthConfig
  * @tparam AuthConfig
  */
class AuthProxyRoutes[AuthConfig <: HeaderAuthConfig](val headerAuthConfig: AuthConfig) {

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](path, upstream, None, requestFilter)

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](path, upstream, None, responseFilter = responseFilter)

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: headerAuthConfig.Permission
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](path, upstream, Option(requiredPermission))

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](path, upstream, Option(requiredPermission), requestFilter)

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](
      path,
      upstream,
      Option(requiredPermission),
      NoOpRequestFilter,
      responseFilter
    )

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]],
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](
      path,
      upstream,
      Option(requiredPermission),
      requestFilter,
      responseFilter
    )

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: Option[headerAuthConfig.Permission] = None,
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]] = NoOpResponseFilter
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    pathPrefix(path) {
      authProxyRoute[AddressingConfig](upstream, requiredPermission, requestFilter, responseFilter)
    }

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: headerAuthConfig.Permission
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, Some(requiredPermission))

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, None, requestFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, None, NoOpRequestFilter, responseFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, Some(requiredPermission), requestFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, Some(requiredPermission), NoOpRequestFilter, responseFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]],
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, None, requestFilter, responseFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]],
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, Option(requiredPermission), requestFilter, responseFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: Option[headerAuthConfig.Permission] = None,
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]] = NoOpResponseFilter
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    extractRequest { request =>
      implicit val reqMeta = RequestMetadata.fromRequest(request)

      authenticateAndProxyRequest[AddressingConfig](
        upstream,
        request,
        requiredPermission,
        requestFilter,
        responseFilter
      )
    }

  private def authenticateAndProxyRequest[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      request: HttpRequest,
      requiredPermission: Option[headerAuthConfig.Permission],
      requestFilter: RequestFilter[Option[headerAuthConfig.AuthData]],
      responseFilter: ResponseFilter[Option[headerAuthConfig.AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig],
      reqMeta: RequestMetadata
    ): Route = {
    authenticate(headerAuthConfig)(requiredPermission)(
      reqMeta,
      ctx.metrics
    ) { optAuthData =>
      filterRequest(requestFilter, optAuthData) {
        addAuthHeader(headerAuthConfig)(optAuthData)(reqMeta) {
          filterResponse(responseFilter, optAuthData) {
            proxyRoute(upstream)
          }
        }
      }
    }
  }

}
