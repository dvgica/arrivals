package com.pagerduty.akka.http.authproxy

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.{HttpProxy, Upstream}
import com.pagerduty.akka.http.support.RequestMetadata

import scala.concurrent.{ExecutionContext, Future}

trait AuthProxyController[AuthConfig <: HeaderAuthConfig, AddressingConfig] {

  implicit def executionContext: ExecutionContext

  val authConfig: AuthConfig
  def httpProxy: HttpProxy[AddressingConfig]
  def headerAuthenticator: HeaderAuthenticator

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requestTransformer: RequestTransformer[AuthConfig#AuthData]
  ): Route =
    prefixProxyRoute(path, upstream, None, Some(requestTransformer))

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      responseTransformer: ResponseTransformer[AuthConfig#AuthData]
  ): Route =
    prefixProxyRoute(path, upstream, None, None, Some(responseTransformer))

  def prefixProxyRoute(path: PathMatcher[Unit],
                       upstream: Upstream[AddressingConfig],
                       requiredPermission: AuthConfig#Permission): Route =
    prefixProxyRoute(path, upstream, Some(requiredPermission))

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestTransformer: RequestTransformer[AuthConfig#AuthData]
  ): Route =
    prefixProxyRoute(path,
                     upstream,
                     Some(requiredPermission),
                     Some(requestTransformer))

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseTransformer: ResponseTransformer[AuthConfig#AuthData]
  ): Route =
    prefixProxyRoute(path,
                     upstream,
                     Some(requiredPermission),
                     None,
                     Some(responseTransformer))

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestTransformer: RequestTransformer[AuthConfig#AuthData],
      responseTransformer: ResponseTransformer[AuthConfig#AuthData]
  ): Route =
    prefixProxyRoute(path,
                     upstream,
                     Some(requiredPermission),
                     Some(requestTransformer),
                     Some(responseTransformer))

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestTransformer: Option[RequestTransformer[AuthConfig#AuthData]] =
        None,
      responseTransformer: Option[ResponseTransformer[AuthConfig#AuthData]] =
        None,
      stripAuthorizationHeader: Boolean = true
  ): Route =
    pathPrefix(path) {
      proxyRoute(upstream,
                 requiredPermission,
                 requestTransformer,
                 responseTransformer,
                 stripAuthorizationHeader)
    }

  def proxyRoute(upstream: Upstream[AddressingConfig],
                 requiredPermission: AuthConfig#Permission): Route =
    proxyRoute(upstream, Some(requiredPermission))

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      requestTransformer: RequestTransformer[AuthConfig#AuthData]): Route =
    proxyRoute(upstream, None, Some(requestTransformer))

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      responseTransformer: ResponseTransformer[AuthConfig#AuthData]): Route =
    proxyRoute(upstream, None, None, Some(responseTransformer))

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestTransformer: RequestTransformer[AuthConfig#AuthData]): Route =
    proxyRoute(upstream, Some(requiredPermission), Some(requestTransformer))

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseTransformer: ResponseTransformer[AuthConfig#AuthData]): Route =
    proxyRoute(upstream,
               Some(requiredPermission),
               None,
               Some(responseTransformer))

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      requestTransformer: RequestTransformer[AuthConfig#AuthData],
      responseTransformer: ResponseTransformer[AuthConfig#AuthData]
  ): Route =
    proxyRoute(upstream,
               None,
               Some(requestTransformer),
               Some(responseTransformer))

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestTransformer: RequestTransformer[AuthConfig#AuthData],
      responseTransformer: ResponseTransformer[AuthConfig#AuthData]
  ): Route =
    proxyRoute(upstream,
               Some(requiredPermission),
               Some(requestTransformer),
               Some(responseTransformer))

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestTransformer: Option[RequestTransformer[AuthConfig#AuthData]] =
        None,
      responseTransformer: Option[ResponseTransformer[AuthConfig#AuthData]] =
        None,
      stripAuthorizationHeader: Boolean = true
  ): Route =
    extractRequest { request =>
      implicit val reqMeta = RequestMetadata.fromRequest(request)

      proxyAuthenticatedRequest(
        upstream,
        request,
        requiredPermission,
        requestTransformer,
        responseTransformer,
        stripAuthorizationHeader
      )
    }

  private def proxyAuthenticatedRequest(
      upstream: Upstream[AddressingConfig],
      request: HttpRequest,
      requiredPermission: Option[AuthConfig#Permission],
      requestTransformer: Option[RequestTransformer[AuthConfig#AuthData]],
      responseTransformer: Option[ResponseTransformer[AuthConfig#AuthData]],
      stripAuthorizationHeader: Boolean
  )(implicit reqMeta: RequestMetadata): Route = {
    complete {
      headerAuthenticator.addAuthHeader(authConfig)(
        request,
        stripAuthorizationHeader,
        requiredPermission.asInstanceOf[Option[authConfig.Permission]]
      ) {
        case (authedRequest, optAuthData) =>
          val proxyRequest = requestTransformer match {
            case Some(transformer) =>
              transformer.transformRequest(authedRequest, optAuthData)
            case None => Future.successful(authedRequest)
          }

          val upstreamResponse =
            proxyRequest.flatMap(httpProxy.request(_, upstream))

          responseTransformer match {
            case Some(transformer) =>
              upstreamResponse.flatMap(
                transformer.transformResponse(_, optAuthData))
            case None =>
              upstreamResponse
          }
      }
    }
  }

}
