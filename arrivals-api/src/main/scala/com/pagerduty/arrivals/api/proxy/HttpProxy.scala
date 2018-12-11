package com.pagerduty.arrivals.api.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import scala.concurrent.Future

trait HttpProxy[AddressingConfig] {
  def apply(request: HttpRequest, upstream: Upstream[AddressingConfig]): Future[HttpResponse]
}
