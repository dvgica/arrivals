package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait ProxyController[AddressingConfig] {

  def httpProxy: HttpProxy[AddressingConfig]

  def proxyRouteUnauthenticated(upstream: Upstream[AddressingConfig]): Route =
    extractRequest { request =>
      complete {
        httpProxy.request(request, upstream)
      }
    }
}
