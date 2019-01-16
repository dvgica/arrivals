package com.pagerduty.arrivals.api.proxy

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}

/** An HTTP server to which requests can be proxied.
  *
  * Requests proxied to an [[Upstream]] go through several steps:
  *
  * 1. [[Upstream.addressRequest]] addresses a request for its destination
  *
  * 2. The defined [[com.pagerduty.arrivals.api.filter.RequestFilter]] is run against the request. This is the last
  *    chance to make modifications before the `HttpRequest` is proxied.
  *
  * 3. If the proxied request returns an `HttpResponse`, the defined
  *    [[com.pagerduty.arrivals.api.filter.ResponseFilter]] is run against it.
  *
  * @tparam AddressingConfig Any configuration needed to address a request to an Upstream. This is typically
  *                          dynamic, runtime information, like the IP address of a load balancer obtained from a
  *                          container scheduler.
  */
trait Upstream[-AddressingConfig] {

  /** Define a `String` with which upstream metrics (e.g. response counts, response duration) will be tagged. */
  def metricsTag: String

  /** Define a method which addresses a request for delivery to an `Upstream`.
    *
    * Incoming requests typically have a URL like `https://example.com/foo`. You likely want to change that URL to
    * something like `http://foo.service/foo`, where `foo.service` is the destination hostname for your request.
    *
    * @param request The incoming request
    * @param addressingConfig Any configuration necessary to address the request
    * @return The addressed request
    */
  def addressRequest(request: HttpRequest, addressingConfig: AddressingConfig): HttpRequest

  /** Override this method to run a [[com.pagerduty.arrivals.api.filter.RequestFilter]] before the request is proxied to the `Upstream`. */
  def requestFilter: RequestFilter[Unit] = NoOpRequestFilter

  /** Override this method to run a [[com.pagerduty.arrivals.api.filter.ResponseFilter]] on the received response. */
  def responseFilter: ResponseFilter[Unit] = NoOpResponseFilter
}
