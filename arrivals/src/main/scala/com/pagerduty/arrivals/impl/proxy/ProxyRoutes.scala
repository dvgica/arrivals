package com.pagerduty.arrivals.impl.proxy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.impl.ArrivalsContext
import com.pagerduty.arrivals.impl.filter.FilterDirectives._

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
