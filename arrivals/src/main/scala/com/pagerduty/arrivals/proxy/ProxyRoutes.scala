package com.pagerduty.arrivals.proxy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.arrivals.ArrivalsContext
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.filter.FilterDirectives._

/** Routes completed by proxying the request to an `Upstream`.
  *
  * There are essentially two variants to the methods here:
  * - `prefixProxyRoute` proxies all requests that have a path starting with the given `path`
  * - `proxyRoute` proxies all requests
  *
  * If a `RequestFilter` is provided, it is run prior to proxying. Since there is no request data available, `Unit` is passed
  * to the filter.
  *
  * If a `ResponseFilter` is provided, it is run on the proxied response.
  */
object ProxyRoutes {

  def prefixProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Unit]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixProxyRoute(path, upstream, NoOpRequestFilter, responseFilter)

  def prefixProxyRoute[AddressingConfig](
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Unit] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Unit] = NoOpResponseFilter
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    pathPrefix(path) {
      proxyRoute(upstream, requestFilter, responseFilter)
    }

  def proxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Unit]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    proxyRoute(upstream, NoOpRequestFilter, responseFilter)

  def proxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Unit] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Unit] = NoOpResponseFilter
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    filterRequest(requestFilter, ()) {
      filterResponse(responseFilter, ()) {
        proxyRoute(upstream)
      }
    }

  def proxyRoute[AddressingConfig](
      upstream: Upstream[AddressingConfig]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route = {
    extractRequest { request =>
      complete {
        ctx.httpProxy(request, upstream)
      }
    }
  }

}
