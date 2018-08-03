package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.{HttpProxy, Upstream}
import com.pagerduty.akka.http.support.RequestMetadata

trait AuthProxyController[AuthConfig <: HeaderAuthConfig] {

  val authConfig: AuthConfig
  def httpProxy: HttpProxy
  def headerAuthenticator: HeaderAuthenticator
  def localHostname: String

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream,
      requestTransformer: HttpRequest => HttpRequest
  ): Route =
    prefixProxyRoute(path, upstream, None, Some(requestTransformer))

  def prefixProxyRoute(path: PathMatcher[Unit],
                       upstream: Upstream,
                       requiredPermission: AuthConfig#Permission): Route =
    prefixProxyRoute(path, upstream, Some(requiredPermission))

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream,
      requiredPermission: AuthConfig#Permission,
      requestTransformer: HttpRequest => HttpRequest
  ): Route =
    prefixProxyRoute(path,
                     upstream,
                     Some(requiredPermission),
                     Some(requestTransformer))

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream,
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestTransformer: Option[HttpRequest => HttpRequest] = None,
      stripAuthorizationHeader: Boolean = true
  ): Route =
    pathPrefix(path) {
      proxyRoute(upstream,
                 requiredPermission,
                 requestTransformer,
                 stripAuthorizationHeader)
    }

  def proxyRoute(upstream: Upstream,
                 requiredPermission: AuthConfig#Permission): Route =
    proxyRoute(upstream, Some(requiredPermission))

  def proxyRoute(upstream: Upstream,
                 requestTransformer: HttpRequest => HttpRequest): Route =
    proxyRoute(upstream, None, Some(requestTransformer))

  def proxyRoute(
      upstream: Upstream,
      requiredPermission: AuthConfig#Permission,
      requestTransformer: HttpRequest => HttpRequest
  ): Route =
    proxyRoute(upstream, Some(requiredPermission), Some(requestTransformer))

  def proxyRoute(
      upstream: Upstream,
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestTransformer: Option[HttpRequest => HttpRequest] = None,
      stripAuthorizationHeader: Boolean = true
  ): Route =
    extractRequest { request =>
      implicit val reqMeta = RequestMetadata.fromRequest(request)

      val proxyRequest = requestTransformer match {
        case Some(transformer) => transformer(request)
        case None => request
      }

      proxyAuthenticatedRequest(
        upstream,
        proxyRequest,
        requiredPermission,
        stripAuthorizationHeader
      )
    }

  private def proxyAuthenticatedRequest(
      upstream: Upstream,
      request: HttpRequest,
      requiredPermission: Option[AuthConfig#Permission],
      stripAuthorizationHeader: Boolean
  )(implicit reqMeta: RequestMetadata): Route = {
    complete {
      headerAuthenticator.addAuthHeader(authConfig)(
        request,
        stripAuthorizationHeader,
        requiredPermission.asInstanceOf[Option[authConfig.Permission]]
      ) { authedRequest =>
        httpProxy.request(authedRequest, upstream)
      }
    }
  }

}
