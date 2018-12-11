package com.pagerduty.arrivals.api.aggregator

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

trait Aggregator[AuthData, RequestKey, AccumulatedState, AddressingConfig] {
  type RequestMap =
    Map[RequestKey, (AggregatorUpstream[AddressingConfig], HttpRequest)]
  type ResponseMap = Map[RequestKey, (HttpResponse, String)]
  type AccumulatedStateAndRequests = (AccumulatedState, RequestMap)
  type HandlerResult = Either[HttpResponse, AccumulatedStateAndRequests]
  type ResponseHandler = (AccumulatedState, ResponseMap) => HandlerResult

  def handleIncomingRequest(incomingRequest: HttpRequest, authData: AuthData): HandlerResult

  def intermediateResponseHandlers: Seq[ResponseHandler]

  def buildOutgoingResponse(accumulatedState: AccumulatedState, upstreamResponses: ResponseMap): HttpResponse
}
