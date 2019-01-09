package com.pagerduty.arrivals.api.proxy

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}

// Upstreams are parameterized on an `AddressingConfig` to support any dynamic configuration you
// may need for your upstream.
// e.g. do you need to do service discovery at runtime? include the IP's in the AddressingConfig.
//
// The lifetime of an upstream request
// 1. address the request.
//    An HTTPRequest needs a place to go, you're job as the Upstream implemeter
//    is to attach a uri to given request
//
// 2. filter request.
//    Last chance to make any modifications to the HTTPRequest before the request is made.
//
// 3. filter response.
//    We've received a response from the prepared request, define a filter before we pass it
//    up to whomever made the request.
trait Upstream[-AddressingConfig] {
  def metricsTag: String

  def addressRequest(request: HttpRequest, addressingConfig: AddressingConfig): HttpRequest

  def requestFilter: RequestFilter[Unit] = NoOpRequestFilter

  def responseFilter: ResponseFilter[Unit] = NoOpResponseFilter
}
