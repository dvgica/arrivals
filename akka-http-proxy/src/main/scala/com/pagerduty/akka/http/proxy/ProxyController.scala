package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pagerduty.akka.http.support.RequestMetadata

trait ProxyController[AddressingConfig] {

  def httpProxy: HttpProxy[AddressingConfig]

  def proxyRouteUnauthenticated(upstream: Upstream[AddressingConfig]): Route =
    extractRequest { request =>
      implicit val reqMeta = RequestMetadata.fromRequest(request)
      complete {
        httpProxy.request(request, upstream)
      }
    }
}
