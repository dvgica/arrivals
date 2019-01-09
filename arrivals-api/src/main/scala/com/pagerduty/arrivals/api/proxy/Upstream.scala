package com.pagerduty.arrivals.api.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.Uri.Authority

// Upstreams are parameterized on an `AddressingConfig` to support any dynamic configuration you
// may need for your upstream.
// e.g. do you need to do service discovery at runtime? include the IP's in the AddressingConfig.
//
// The lifetime of an upstream request
// 1. address the request.
//    An HTTPRequest needs a place to go, you're job as the Upstream implemeter
//    is to attach a uri to given request
//
// 2. prepare request.
//    Last chance to make any modifications to the HTTPRequest before the request is made.
//
// 3. transform response.
//    We've received a response from the prepared request, define a transform before we pass it
//    up to whomever made the request.
trait Upstream[-AddressingConfig] {
  def metricsTag: String

  def addressRequest(request: HttpRequest, addressingConfig: AddressingConfig): HttpRequest

  def prepareRequestForDelivery(request: HttpRequest): HttpRequest = request

  def transformResponse(request: HttpRequest, response: HttpResponse): HttpResponse = response
}
