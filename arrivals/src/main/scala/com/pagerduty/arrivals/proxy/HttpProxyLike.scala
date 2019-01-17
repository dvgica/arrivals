package com.pagerduty.arrivals.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.api.proxy.Upstream

import scala.concurrent.Future

/** An interface for [[HttpProxy]], useful for unit testing. */
trait HttpProxyLike[AddressingConfig] {
  def apply(request: HttpRequest, upstream: Upstream[AddressingConfig]): Future[HttpResponse]
}
