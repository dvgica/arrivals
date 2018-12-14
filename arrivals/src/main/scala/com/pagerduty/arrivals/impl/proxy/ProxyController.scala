package com.pagerduty.arrivals.impl.proxy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}
import com.pagerduty.arrivals.api.proxy.Upstream
import com.pagerduty.arrivals.impl.filter.FilterDirectives._

trait ProxyController[AddressingConfig] {

  def httpProxy: api.proxy.HttpProxy[AddressingConfig]

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      responseFilter: ResponseFilter[Unit]
    ): Route =
    prefixProxyRoute(path, upstream, NoOpRequestFilter, responseFilter)

  def prefixProxyRoute(
      path: PathMatcher[Unit],
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Unit] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Unit] = NoOpResponseFilter
    ): Route =
    pathPrefix(path) {
      proxyRoute(upstream, requestFilter, responseFilter)
    }

  def proxyRoute(upstream: Upstream[AddressingConfig], responseFilter: ResponseFilter[Unit]): Route =
    proxyRoute(upstream, NoOpRequestFilter, responseFilter)

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Unit] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Unit] = NoOpResponseFilter
    ): Route =
    filterRequest(requestFilter, ()) {
      filterResponse(responseFilter, ()) {
        extractRequest { filteredRequest =>
          implicit val reqMeta = RequestMetadata.fromRequest(filteredRequest)
          complete {
            httpProxy(filteredRequest, upstream)
          }
        }
      }
    }

}
