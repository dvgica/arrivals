package com.pagerduty.arrivals.api.aggregator

trait TwoStepAggregator[
    AuthData, RequestKey, AccumulatedState, AddressingConfig]
    extends Aggregator[AuthData,
                       RequestKey,
                       AccumulatedState,
                       AddressingConfig] {

  def handleUpstreamResponses(initialState: AccumulatedState,
                              upstreamResponseMap: ResponseMap): HandlerResult

  def intermediateResponseHandlers: Seq[ResponseHandler] =
    Seq(handleUpstreamResponses)

}
