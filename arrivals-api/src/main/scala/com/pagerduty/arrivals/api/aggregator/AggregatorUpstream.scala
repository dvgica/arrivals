package com.pagerduty.arrivals.api.aggregator

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.api.proxy.Upstream

trait AggregatorUpstream[AddressingConfig] extends Upstream[AddressingConfig] {
  def prepareAggregatorRequestForDelivery(
      authConfig: HeaderAuthConfig,
      request: HttpRequest,
      modelRequest: HttpRequest
    ): HttpRequest = request
}
