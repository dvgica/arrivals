package com.pagerduty.akka.http.aggregator.aggregator

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationConfig

trait OneStepAggregator[RequestKey, AddressingConfig]
    extends GenericAggregator[RequestKey, NotUsed, AddressingConfig] {

  def intermediateResponseHandlers: Seq[ResponseHandler] = Seq()

  def buildOutgoingResponse(accumulatedState: NotUsed,
                            upstreamResponses: ResponseMap): HttpResponse = {
    buildOutgoingResponseStateless(upstreamResponses)
  }

  def buildOutgoingResponseStateless(
      upstreamResponses: ResponseMap): HttpResponse
}
