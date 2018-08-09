package com.pagerduty.akka.http.aggregator.aggregator

trait TwoStepAggregator[RequestKey, AccumulatedState, AddressingConfig]
    extends Aggregator[RequestKey, AccumulatedState, AddressingConfig] {

  def handleUpstreamResponses(
      initialState: AccumulatedState,
      upstreamResponseMap: ResponseMap
  ): (AccumulatedState, RequestMap)

  def intermediateResponseHandlers: Seq[ResponseHandler] =
    Seq(handleUpstreamResponses)

}
