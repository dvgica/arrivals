package com.pagerduty.akka.http.aggregator

import akka.http.scaladsl.model.{HttpHeader, HttpRequest}
import akka.http.scaladsl.model.headers._
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationData.AuthFailedReason
import com.pagerduty.akka.http.support.RequestMetadata
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future
import scala.util.Try

class AggregatorUpstreamSpec extends FreeSpecLike with Matchers {
  val authHeader = RawHeader("X-Authentication", "test-auth")

  class TestAuthConfig extends HeaderAuthConfig {
    type Cred = String
    type AuthData = String
    type Permission = String

    def extractCredentials(request: HttpRequest)(
        implicit reqMeta: RequestMetadata): List[Cred] = ???

    def authenticate(credential: Cred)(
        implicit reqMeta: RequestMetadata): Future[Try[Option[AuthData]]] = ???

    def authDataGrantsPermission(
        authData: AuthData,
        request: HttpRequest,
        permission: Option[Permission]
    )(implicit reqMeta: RequestMetadata): Option[AuthFailedReason] = ???

    def dataToAuthHeader(data: AuthData)(
        implicit reqMeta: RequestMetadata): HttpHeader = ???
    def authHeaderName: String = authHeader.name
  }

  val ac = new TestAuthConfig

  "AggregatorUpstream" - {

    val u = new AggregatorUpstream[String] {
      val metricsTag = "test"
      def addressRequest(request: HttpRequest,
                         localHostname: String): HttpRequest = ???
    }

    "prepares an aggregator request for delivery" in {
      val request = HttpRequest()
      val modelRequest = HttpRequest().addHeader(authHeader)

      val preparedReq =
        u.prepareAggregatorRequestForDelivery(ac, request, modelRequest)

      preparedReq.headers
        .find(_.is(authHeader.lowercaseName))
        .get should equal(authHeader)
    }
  }
}
