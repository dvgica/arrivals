package com.pagerduty.akka.http.aggregator

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.Upstream
import com.pagerduty.akka.http.support.RequestIdHeader

trait AggregatorUpstream[AddressingConfig] extends Upstream[AddressingConfig] {
  def prepareAggregatorRequestForDelivery(
      authConfig: HeaderAuthConfig,
      request: HttpRequest,
      modelRequest: HttpRequest
  ): HttpRequest = {
    val reqWithMaybeIdHeader = modelRequest.header[RequestIdHeader] match {
      case Some(h) => request.addHeader(h)
      case None => request
    }

    // we will always have an auth header by this point
    val authHeader = modelRequest.headers
      .find(_.is(authConfig.authHeaderName.toLowerCase))
      .get

    reqWithMaybeIdHeader.addHeader(authHeader)
  }
}
