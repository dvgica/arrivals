package com.pagerduty.akka.http.aggregator.aggregator

trait TwoStepAggregator[RequestKey, AccumulatedState, AddressingConfig]
    extends Aggregator[RequestKey, AccumulatedState, AddressingConfig] {

  def handleUpstreamResponses(
      handlerInput: HandlerInput
  ): HandlerResult

  def intermediateResponseHandlers: Seq[ResponseHandler] =
    Seq(handleUpstreamResponses)

}
