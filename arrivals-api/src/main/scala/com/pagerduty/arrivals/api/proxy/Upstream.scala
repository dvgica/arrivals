package com.pagerduty.arrivals.api.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.Uri.Authority

trait Upstream[AddressingConfig] {
  def metricsTag: String

  def addressRequest(request: HttpRequest,
                     addressingConfig: AddressingConfig): HttpRequest

  def prepareRequestForDelivery(request: HttpRequest): HttpRequest = request

  def transformResponse(request: HttpRequest,
                        response: HttpResponse): HttpResponse = response
}
