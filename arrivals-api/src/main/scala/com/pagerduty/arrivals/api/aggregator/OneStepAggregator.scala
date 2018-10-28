package com.pagerduty.arrivals.api.aggregator

import akka.NotUsed
import akka.http.scaladsl.model.HttpResponse

trait OneStepAggregator[AuthData, RequestKey, AddressingConfig]
    extends Aggregator[AuthData, RequestKey, NotUsed, AddressingConfig] {

  def intermediateResponseHandlers: Seq[ResponseHandler] = Seq()

  def buildOutgoingResponse(accumulatedState: NotUsed,
                            upstreamResponses: ResponseMap): HttpResponse = {
    buildOutgoingResponseStateless(upstreamResponses)
  }

  def buildOutgoingResponseStateless(
      upstreamResponses: ResponseMap): HttpResponse
}
