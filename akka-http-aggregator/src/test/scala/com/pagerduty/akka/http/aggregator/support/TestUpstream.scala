package com.pagerduty.akka.http.aggregator.support

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.akka.http.aggregator.AggregatorUpstream
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.CommonHostnameUpstream

case class TestUpstream(port: Int, metricsTag: String)
    extends CommonHostnameUpstream
    with AggregatorUpstream[String] {
  override def prepareAggregatorRequestForDelivery(
      authConfig: HeaderAuthConfig,
      request: HttpRequest,
      modelRequest: HttpRequest
  ): HttpRequest =
    request
}
