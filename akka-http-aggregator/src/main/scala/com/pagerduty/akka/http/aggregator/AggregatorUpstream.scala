package com.pagerduty.akka.http.aggregator

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.Upstream

trait AggregatorUpstream[AddressingConfig] extends Upstream[AddressingConfig] {
  def prepareAggregatorRequestForDelivery(
      authConfig: HeaderAuthConfig,
      request: HttpRequest,
      modelRequest: HttpRequest
  ): HttpRequest = {
    // we will always have an auth header by this point
    val authHeader = modelRequest.headers
      .find(_.is(authConfig.authHeaderName.toLowerCase))
      .get
    request.addHeader(authHeader)
  }
}
