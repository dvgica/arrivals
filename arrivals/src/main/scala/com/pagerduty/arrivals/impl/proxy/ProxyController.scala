package com.pagerduty.arrivals.impl.proxy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pagerduty.arrivals.api.filter.{
  NoOpRequestFilter,
  NoOpResponseFilter,
  RequestFilter,
  ResponseFilter
}
import com.pagerduty.arrivals.api.proxy.Upstream

trait ProxyController[AddressingConfig] {

  def httpProxy: HttpProxy[AddressingConfig]
  def proxyRequestHandler: ProxyRequestHandler[AddressingConfig]

  def proxyRoute(upstream: Upstream[AddressingConfig],
                 responseFilter: ResponseFilter[Unit]): Route =
    proxyRoute(upstream, NoOpRequestFilter, responseFilter)

  def proxyRoute(
      upstream: Upstream[AddressingConfig],
      requestFilter: RequestFilter[Unit] = NoOpRequestFilter,
      responseFilter: ResponseFilter[Unit] = NoOpResponseFilter): Route =
    extractRequest { request =>
      complete {
        proxyRequestHandler.apply(request,
                                  upstream,
                                  (),
                                  httpProxy,
                                  requestFilter,
                                  responseFilter)
      }
    }
}
