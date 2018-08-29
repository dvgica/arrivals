package com.pagerduty.akka.http.aggregator.aggregator

trait TwoStepAggregator[
    AuthData, RequestKey, AccumulatedState, AddressingConfig]
    extends GenericAggregator[AuthData,
                              RequestKey,
                              AccumulatedState,
                              AddressingConfig] {

  def handleUpstreamResponses(initialState: AccumulatedState,
                              upstreamResponseMap: ResponseMap): HandlerResult

  def intermediateResponseHandlers: Seq[ResponseHandler] =
    Seq(handleUpstreamResponses)

}
