package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.pagerduty.akka.http.proxy.filter.{
  NoOpRequestFilter,
  NoOpResponseFilter,
  RequestFilter,
  ResponseFilter
}

trait ProxyController[AddressingConfig, RequestData] {

  def httpProxy: HttpProxy[AddressingConfig]
  def proxyRequestHandler
    : RequestHandler[Upstream[AddressingConfig], RequestData]

  def proxyRoute(upstream: Upstream[AddressingConfig],
                 responseFilter: ResponseFilter[RequestData]): Route =
    proxyRoute(upstream, NoOpRequestFilter, responseFilter)

  def proxyRoute(upstream: Upstream[AddressingConfig],
                 requestFilter: RequestFilter[RequestData] = NoOpRequestFilter,
                 responseFilter: ResponseFilter[RequestData] =
                   NoOpResponseFilter,
                 requestData: Option[RequestData] = None): Route =
    extractRequest { request =>
      complete {
        proxyRequestHandler.handle(request,
                                   upstream,
                                   httpProxy,
                                   requestFilter,
                                   responseFilter,
                                   requestData)
      }
    }
}
