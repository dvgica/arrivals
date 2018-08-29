package com.pagerduty.akka.http.aggregator

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import akka.stream.Materializer
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.aggregator.aggregator.Aggregator
import com.pagerduty.akka.http.proxy.HttpProxy
import com.pagerduty.akka.http.support.RequestMetadata

import scala.concurrent.ExecutionContext

trait AggregatorController[AuthConfig <: HeaderAuthConfig, AddressingConfig] {
  val authConfig: AuthConfig
  def headerAuthenticator: HeaderAuthenticator
  implicit def httpProxy: HttpProxy[AddressingConfig]
  implicit def executionContext: ExecutionContext
  implicit def materializer: Materializer

  def prefixAggregatorRoute(
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None
  ): Route = {
    pathPrefix(pathMatcher) {
      aggregatorRoute(aggregator, requiredPermission)
    }
  }

  def aggregatorRoute(
      aggregator: Aggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None
  ): Route = {
    extractRequest { incomingRequest =>
      complete {
        implicit val reqMeta = RequestMetadata.fromRequest(incomingRequest)

        headerAuthenticator.addAndRequireAuthHeader(authConfig)(
          incomingRequest,
          false,
          requiredPermission.asInstanceOf[Option[authConfig.Permission]]
        ) { (authedRequest, authData) =>
          aggregator.execute(authConfig)(authedRequest, authData)
        }
      }
    }
  }
}
