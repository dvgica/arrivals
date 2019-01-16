package com.pagerduty.arrivals.api.aggregator

import akka.NotUsed
import akka.http.scaladsl.model.HttpResponse

/** An [[Aggregator]] that builds a response from a single set of upstream requests, and does not need to accumulate state.
  *
  * @tparam AuthData The type of authentication data that this aggregator will provide
  * @tparam RequestKey The type of key to use as a unique identification for generated requests
  * @tparam AddressingConfig The type of configuration needed to proxy requests upstream
  */
trait OneStepAggregator[AuthData, RequestKey, AddressingConfig]
    extends Aggregator[AuthData, RequestKey, NotUsed, AddressingConfig] {

  def intermediateResponseHandlers: Seq[ResponseHandler] = Seq()

  def buildOutgoingResponse(accumulatedState: NotUsed, upstreamResponses: ResponseMap): HttpResponse = {
    buildOutgoingResponseStateless(upstreamResponses)
  }

  /** Build an outgoing response based on the responses to all upstream requests. */
  def buildOutgoingResponseStateless(upstreamResponses: ResponseMap): HttpResponse
}
