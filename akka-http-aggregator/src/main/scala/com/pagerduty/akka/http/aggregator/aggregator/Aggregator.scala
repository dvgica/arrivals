package com.pagerduty.akka.http.aggregator.aggregator

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.aggregator.AggregatorUpstream
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationConfig

trait Aggregator[RequestKey, AccumulatedState] {
  type RequestMap = Map[RequestKey, (AggregatorUpstream, HttpRequest)]
  type ResponseMap = Map[RequestKey, (HttpResponse, String)]
  type ResponseHandler =
    (AccumulatedState, ResponseMap) => (AccumulatedState, RequestMap)

  def handleIncomingRequest(
      authConfig: AuthenticationConfig
  )(incomingRequest: HttpRequest,
    authData: authConfig.AuthData): (AccumulatedState, RequestMap)

  def intermediateResponseHandlers: Seq[ResponseHandler]

  def buildOutgoingResponse(accumulatedState: AccumulatedState,
                            upstreamResponses: ResponseMap): HttpResponse
}
