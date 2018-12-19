package com.pagerduty.arrivals.example

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.model.Uri.Authority
import com.pagerduty.arrivals.api.aggregator.AggregatorUpstream
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig

object ExampleUpstream {
  val CatsUpstream = ExampleUpstream(22000, "cats")
  val DogsUpstream = ExampleUpstream(33000, "dogs")
}

case class ExampleUpstream(port: Int, metricsTag: String) extends AggregatorUpstream[String] {
  def addressRequest(request: HttpRequest, addressingConfig: String): HttpRequest = {
    val newUri =
      request.uri
        .withAuthority(Authority(Uri.Host(addressingConfig), port))
        .withScheme("http")
    request.withUri(newUri)
  }

  override def prepareAggregatorRequestForDelivery(
      authConfig: HeaderAuthConfig,
      request: HttpRequest,
      modelRequest: HttpRequest
    ): HttpRequest = {
    // take the auth header from the original request, and copy it to the new aggregator request
    val authHeader = modelRequest.headers
      .find(_.is(authConfig.authHeaderName.toLowerCase))
      .get

    request.addHeader(authHeader)
  }
}
