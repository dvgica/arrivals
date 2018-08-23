package com.pagerduty.akka.http.aggregator.aggregator

trait TwoStepAggregator[RequestKey, AccumulatedState, AddressingConfig]
    extends GenericAggregator[RequestKey, AccumulatedState, AddressingConfig] {

  def handleUpstreamResponses(initialState: AccumulatedState,
                              upstreamResponseMap: ResponseMap): HandlerResult

  def intermediateResponseHandlers: Seq[ResponseHandler] =
    Seq(handleUpstreamResponses)

}
