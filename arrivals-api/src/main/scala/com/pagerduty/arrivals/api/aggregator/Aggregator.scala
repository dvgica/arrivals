package com.pagerduty.arrivals.api.aggregator

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

/** Based on user-defined logic, splits an incoming request into multiple requests for proxying to various [[AggregatorUpstream]]s.
  * Collects the responses from all such requests and combines them into a single response, again based on user-defined logic.
  *
  * `Aggregator`s are used in `AggregatorRoutes`. They can have an arbitrary number
  * of steps, meaning that more requests can be made after an initial batch, more requests can be done after the second batch, etc.
  *
  * `Aggregator`s are meant to be stateless, and accumulated state is passed between steps explicitly. However, there is nothing
  * stopping the user from implementing the `Aggregator` as a `class` and storing state in `private var`s. In this case, users must
  * be sure to not share `Aggregator` instances between different requests, instead making a new instance for each request.
  *
  * There are several subtypes of `Aggregator` available for more specific circumstances.
  *
  * @tparam AuthData The type of authentication data that this aggregator will provide
  * @tparam RequestKey The type of key to use as a unique identification for generated requests
  * @tparam AccumulatedState The type of state to store between steps of the aggregator
  * @tparam AddressingConfig The type of configuration needed to proxy requests upstream
  */
trait Aggregator[AuthData, RequestKey, AccumulatedState, AddressingConfig] {

  /** A Map of requests to proxy to various `AggregatorUpstream`s */
  type RequestMap =
    Map[RequestKey, (AggregatorUpstream[AddressingConfig], HttpRequest)]

  /** A Map of the responses to the requests in [[RequestMap]] */
  type ResponseMap = Map[RequestKey, (HttpResponse, String)]

  /** A Tuple combining state to accomulate and requests to execute */
  type AccumulatedStateAndRequests = (AccumulatedState, RequestMap)

  /** The result of a "step" in the `Aggregator` (either the first or subsequent).
    *
    * If the handler returns an `HttpResponse`, the following steps in the `Aggregator` are skipped, and the response
    * is returned directly to the client. Otherwise, another round of requests will be executed.
    */
  type HandlerResult = Either[HttpResponse, AccumulatedStateAndRequests]

  /** A function that handles a Map of responses from various `AggregatorUpstream`s, as well as previously accumulated
    * state.
    */
  type ResponseHandler = (AccumulatedState, ResponseMap) => HandlerResult

  /** Define the first step of the `Aggregator` which handles the incoming request.
    *
    * @param incomingRequest The incoming request that triggered the aggregator
    * @param authData Data found during the successful authentication of the incoming request
    * @return Either a response (ending the `Aggregator`), or a map of requests to proxy to `AggregatorUpstream`s, along with accumulated state.
    */
  def handleIncomingRequest(incomingRequest: HttpRequest, authData: AuthData): HandlerResult

  /** Define an arbitrary number of ordered next steps in the `Aggregator`. These will be executed when the results of the
    * first step are available, one after the other, waiting for results in between.
    */
  def intermediateResponseHandlers: Seq[ResponseHandler]

  /** Define the final step of the `Aggregator` which produces an `HttpResponse` for the client. */
  def buildOutgoingResponse(accumulatedState: AccumulatedState, upstreamResponses: ResponseMap): HttpResponse
}
