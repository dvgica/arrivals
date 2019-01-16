package com.pagerduty.arrivals.api.aggregator

/** An [[Aggregator]] that works in two steps, first sending a set of requests upstream, then sending another set
  * based on the responses to the first set. A response is built based on all received responses.
  *
  * @tparam AuthData The type of authentication data that this aggregator will provide
  * @tparam RequestKey The type of key to use as a unique identification for generated requests
  * @tparam AccumulatedState The type of state to store between steps of the aggregator
  * @tparam AddressingConfig The type of configuration needed to proxy requests upstream
  */
trait TwoStepAggregator[AuthData, RequestKey, AccumulatedState, AddressingConfig]
    extends Aggregator[AuthData, RequestKey, AccumulatedState, AddressingConfig] {

  /** Handle the first set of upstream responses, along with accumulated state. */
  def handleUpstreamResponses(initialState: AccumulatedState, upstreamResponseMap: ResponseMap): HandlerResult

  def intermediateResponseHandlers: Seq[ResponseHandler] =
    Seq(handleUpstreamResponses)

}
