package com.pagerduty.arrivals.impl.authproxy

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.impl.ArrivalsContext
import com.pagerduty.arrivals.impl.auth.AuthenticationDirectives._
import com.pagerduty.arrivals.impl.headerauth.AuthHeaderDirectives._
import com.pagerduty.arrivals.impl.filter.FilterDirectives._

class AuthProxyDirectives[AuthConfig <: HeaderAuthConfig](headerAuthConfig: AuthConfig) {

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](path, upstream, None, requestFilter)

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](path, upstream, None, responseFilter = responseFilter)

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](path, upstream, Option(requiredPermission))

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAuthProxyRoute[AddressingConfig](path, upstream, Option(requiredPermission), requestFilter)

  def prefixAuthProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
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
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
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
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]] = NoOpResponseFilter
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    pathPrefix(path) {
      authProxyRoute[AddressingConfig](upstream, requiredPermission, requestFilter, responseFilter)
    }

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, Some(requiredPermission))

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, None, requestFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, None, NoOpRequestFilter, responseFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, Some(requiredPermission), requestFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, Some(requiredPermission), NoOpRequestFilter, responseFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, None, requestFilter, responseFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    authProxyRoute[AddressingConfig](upstream, Option(requiredPermission), requestFilter, responseFilter)

  def authProxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]] = NoOpResponseFilter
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
      requiredPermission: Option[AuthConfig#Permission],
      requestFilter: RequestFilter[Option[AuthConfig#AuthData]],
      responseFilter: ResponseFilter[Option[AuthConfig#AuthData]]
    )(implicit ctx: ArrivalsContext[AddressingConfig],
      reqMeta: RequestMetadata
    ): Route = {
    authenticate(headerAuthConfig)(requiredPermission.asInstanceOf[Option[headerAuthConfig.Permission]])(
      reqMeta,
      ctx.metrics
    ) { optAuthData =>
      filterRequest(requestFilter, optAuthData) {
        addAuthHeader(headerAuthConfig)(optAuthData.asInstanceOf[Option[headerAuthConfig.AuthData]])(reqMeta) {
          filterResponse(responseFilter, optAuthData) {
            extractRequest { authedRequest =>
              complete {
                ctx.httpProxy(authedRequest, upstream)
              }
            }
          }
        }
      }
    }
  }

}
