package com.pagerduty.akka.http.aggregator.aggregator

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.HttpProxy

import scala.concurrent.{ExecutionContext, Future}

trait Aggregator[AuthData, AddressingConfig] {
  def execute(authConfig: HeaderAuthConfig)(authedRequest: HttpRequest,
                                            authData: AuthData)(
      implicit httpProxy: HttpProxy[AddressingConfig],
      executionContext: ExecutionContext,
      materializer: Materializer): Future[HttpResponse]
}
