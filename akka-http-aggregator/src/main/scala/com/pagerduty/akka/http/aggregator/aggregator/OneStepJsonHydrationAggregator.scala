package com.pagerduty.akka.http.aggregator.aggregator

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import ujson.Js

/**
  * This Aggregator can be used when the API gateway needs to make multiple parallel upstream requests based on the
  * incoming request, and will use the JSON upstream responses to build a single response to the user.
  *
  * Motivating Use Case:
  * - fetch an entity along with that entity's children (stored in a separate service), return in a single response
  *
  * Assumptions:
  * - All upstream requests can be made in parallel
  * - All upstream responses will be JSON
  * - No part of the incoming request or auth data needs to be saved in order to build the outgoing response
  * - It's OK if an error parsing upstream responses into JSON results in a 500 (no partial degradation)
  *
  * If any of these assumptions are untrue, look at using a supertype of this trait instead.
  */
trait OneStepJsonHydrationAggregator[AuthData, AddressingConfig]
    extends OneStepAggregator[AuthData, String, AddressingConfig] {

  // implement these two methods
  def handleIncomingRequestStateless(
      incomingRequest: HttpRequest,
      authData: AuthData): Either[HttpResponse, RequestMap]

  def buildOutgoingJsonResponseStateless(
      upstreamJsonResponses: Map[String, (HttpResponse, Js.Value)])
    : HttpResponse

  // the rest is internal implementation
  override def handleIncomingRequest(incomingRequest: HttpRequest,
                                     authData: AuthData): HandlerResult = {
    handleIncomingRequestStateless(incomingRequest, authData) match {
      case Right(requests) => Right(NotUsed, requests)
      case Left(response) => Left(response)
    }
  }

  override def buildOutgoingResponseStateless(
      upstreamResponses: ResponseMap): HttpResponse = {
    val upstreamJsonResponses = upstreamResponses.map {
      case (requestKey, (response, entity)) =>
        (requestKey, (response, ujson.read(entity)))
    }
    buildOutgoingJsonResponseStateless(upstreamJsonResponses)
  }
}
