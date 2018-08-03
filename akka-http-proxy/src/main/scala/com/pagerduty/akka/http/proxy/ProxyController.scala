package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait ProxyController {

  def httpProxy: HttpProxy

  def proxyRouteUnauthenticated[U <: Upstream](upstream: U): Route =
    extractRequest { request =>
      complete {
        httpProxy.request(request, upstream)
      }
    }
}
