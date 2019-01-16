package com.pagerduty.arrivals.impl.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.api.proxy.Upstream

import scala.concurrent.Future

trait HttpProxyLike[AddressingConfig] {
  def apply(request: HttpRequest, upstream: Upstream[AddressingConfig]): Future[HttpResponse]
}
