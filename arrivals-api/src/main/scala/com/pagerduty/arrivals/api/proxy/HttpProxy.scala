package com.pagerduty.arrivals.api.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

// HttpProxy drives a given HttpRequest through an Upstream's Request Lifetime.
// see ./Upstream.scala for the steps an HttpProxy needs to take to process an upstream request.
trait HttpProxy[AddressingConfig] {
  def apply(request: HttpRequest, upstream: Upstream[AddressingConfig]): Future[HttpResponse]
}
