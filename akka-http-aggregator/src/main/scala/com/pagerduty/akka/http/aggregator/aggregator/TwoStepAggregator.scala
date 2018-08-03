package com.pagerduty.akka.http.aggregator.aggregator

trait TwoStepAggregator[RequestKey, AccumulatedState]
    extends Aggregator[RequestKey, AccumulatedState] {

  def handleUpstreamResponses(
      initialState: AccumulatedState,
      upstreamResponseMap: ResponseMap
  ): (AccumulatedState, RequestMap)

  def intermediateResponseHandlers: Seq[ResponseHandler] =
    Seq(handleUpstreamResponses)

}
