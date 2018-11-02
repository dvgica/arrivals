package com.pagerduty.arrivals.impl.aggregator

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import akka.stream.Materializer
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.aggregator.AggregatorDependencies
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.impl.headerauth.HeaderAuthenticator
import com.pagerduty.arrivals.api.proxy.HttpProxy

import scala.concurrent.ExecutionContext

trait AggregatorController[AuthConfig <: HeaderAuthConfig, AddressingConfig] {
  val authConfig: AuthConfig
  def headerAuthenticator: HeaderAuthenticator
  implicit def httpProxy: HttpProxy[AddressingConfig]
  def aggregatorRequestHandler: AggregatorRequestHandler[AddressingConfig, AuthConfig#AuthData]

  implicit def executionContext: ExecutionContext
  implicit def materializer: Materializer

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: AuthConfig#Permission
    ): Route =
    prefixAggregatorRoute(pathMatcher, aggregator, Option(requiredPermission))

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    ): Route =
    prefixAggregatorRoute(pathMatcher, aggregator, None, NoOpRequestFilter, responseFilter)

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData]
    ): Route =
    prefixAggregatorRoute(pathMatcher, aggregator, None, requestFilter)

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    ): Route =
    prefixAggregatorRoute(pathMatcher, aggregator, None, requestFilter, responseFilter)

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    ): Route =
    prefixAggregatorRoute(pathMatcher, aggregator, Option(requiredPermission), NoOpRequestFilter, responseFilter)

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData]
    ): Route =
    prefixAggregatorRoute(pathMatcher, aggregator, Option(requiredPermission), requestFilter)

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    ): Route =
    prefixAggregatorRoute(pathMatcher, aggregator, Option(requiredPermission), requestFilter, responseFilter)

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[AuthConfig#AuthData] = NoOpRequestFilter,
      responseFilter: ResponseFilter[AuthConfig#AuthData] = NoOpResponseFilter
    ): Route = {
    pathPrefix(pathMatcher) {
      aggregatorRoute(aggregator, requiredPermission, requestFilter, responseFilter)
    }
  }

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: AuthConfig#Permission
    ): Route =
    aggregatorRoute(aggregator, Option(requiredPermission))

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData]
    ): Route =
    aggregatorRoute(aggregator, Option(requiredPermission), requestFilter)

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    ): Route =
    aggregatorRoute(aggregator, Option(requiredPermission), NoOpRequestFilter, responseFilter)

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData]
    ): Route =
    aggregatorRoute(aggregator, None, requestFilter)

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    ): Route =
    aggregatorRoute(aggregator, None, NoOpRequestFilter, responseFilter)

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    ): Route =
    aggregatorRoute(aggregator, None, requestFilter, responseFilter)

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    ): Route =
    aggregatorRoute(aggregator, Option(requiredPermission), requestFilter, responseFilter)

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[AuthConfig#AuthData, RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[AuthConfig#AuthData] = NoOpRequestFilter,
      responseFilter: ResponseFilter[AuthConfig#AuthData] = NoOpResponseFilter
    ): Route = {
    extractRequest { incomingRequest =>
      complete {
        implicit val reqMeta = RequestMetadata.fromRequest(incomingRequest)

        headerAuthenticator.addAndRequireAuthHeader(authConfig)(
          incomingRequest,
          requiredPermission.asInstanceOf[Option[authConfig.Permission]]
        ) { (authedRequest, authData) =>
          aggregatorRequestHandler.apply(
            authedRequest,
            AggregatorDependencies(authConfig, httpProxy, executionContext, materializer),
            authData,
            aggregator,
            requestFilter,
            responseFilter
          )
        }
      }
    }
  }
}
