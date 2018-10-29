package com.pagerduty.arrivals.example

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.impl.aggregator.OneStepAggregator

object ExampleAggregator extends OneStepAggregator[Int, String, String] {
  import ExampleGateway._

  override def handleIncomingRequest(incomingRequest: HttpRequest,
                                     authData: Int): HandlerResult = {

    val requests = Map(
      "cats" -> (CatsUpstream, HttpRequest(uri = "/api/cats")),
      "dogs" -> (DogsUpstream, HttpRequest(uri = "/api/dogs"))
    )
    Right((NotUsed, requests))
  }

  override def buildOutgoingResponseStateless(
      upstreamResponses: ResponseMap): HttpResponse = {
    val (_, catBody) = upstreamResponses("cats")
    val (_, dogBody) = upstreamResponses("dogs")

    HttpResponse().withEntity(catBody + "\n" + dogBody)
  }
}
