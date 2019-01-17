package com.pagerduty.arrivals.aggregator.support

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.arrivals.api.aggregator.AggregatorUpstream

case class TestUpstream(port: Int, metricsTag: String) extends AggregatorUpstream[String] {
  def addressRequest(request: HttpRequest, addressingConfig: String): HttpRequest = request
}
