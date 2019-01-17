package com.pagerduty.arrivals.aggregator

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait RunnableAggregator[AuthData, AddressingConfig] {
  def apply(
      authedRequest: HttpRequest,
      deps: AggregatorDependencies[AddressingConfig],
      authData: AuthData
    ): Future[HttpResponse]
}
