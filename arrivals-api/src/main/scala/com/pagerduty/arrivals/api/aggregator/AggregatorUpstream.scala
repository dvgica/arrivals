package com.pagerduty.arrivals.api.aggregator

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.api.proxy.Upstream

/** An [[com.pagerduty.arrivals.api.proxy.Upstream]] that may be used as a target for [[Aggregator]] requests.
  *
  * Requests to `AggregatorUpstream`s may need modifications, such as proof of authentication. This trait allows for
  * copying of characteristics (e.g. headers) from the initial incoming request, on to the multiple requests that are
  * proxied upstream due to a step of an [[Aggregator]].
  *
  * @tparam AddressingConfig Any configuration needed to address a request to an Upstream. This is typically
  *                          dynamic, runtime information, like the IP address of a load balancer obtained from a
  *                          container scheduler.
  */
trait AggregatorUpstream[AddressingConfig] extends Upstream[AddressingConfig] {

  /** Override this method to modify a request for proxying to an [[AggregatorUpstream]].
    *
    * @param authConfig The authentication config
    * @param request The request that will be made to the [[AggregatorUpstream]]
    * @param modelRequest The incoming request that is being split by the [[Aggregator]]
    * @return The new request destined for the [[AggregatorUpstream]]
    */
  def prepareAggregatorRequestForDelivery(
      authConfig: HeaderAuthConfig,
      request: HttpRequest,
      modelRequest: HttpRequest
    ): HttpRequest = request
}
