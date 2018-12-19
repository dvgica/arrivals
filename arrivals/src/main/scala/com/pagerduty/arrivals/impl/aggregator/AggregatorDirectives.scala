package com.pagerduty.arrivals.impl.aggregator

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import com.pagerduty.arrivals.api.filter.{NoOpRequestFilter, NoOpResponseFilter, RequestFilter, ResponseFilter}
import com.pagerduty.akka.http.support.RequestMetadata
import com.pagerduty.arrivals.api.headerauth.HeaderAuthConfig
import com.pagerduty.arrivals.impl.ArrivalsContext
import com.pagerduty.arrivals.impl.auth.AuthenticationDirectives._
import com.pagerduty.arrivals.impl.filter.FilterDirectives.{filterRequest, filterResponse}
import com.pagerduty.arrivals.impl.headerauth.AuthHeaderDirectives._

class AggregatorDirectives[AuthConfig <: HeaderAuthConfig](headerAuthConfig: AuthConfig) {

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: AuthConfig#Permission
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAggregatorRoute[AddressingConfig](
      pathMatcher,
      aggregator,
      Option(requiredPermission)
    )

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAggregatorRoute[AddressingConfig](
      pathMatcher,
      aggregator,
      None,
      NoOpRequestFilter,
      responseFilter
    )

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAggregatorRoute[AddressingConfig](
      pathMatcher,
      aggregator,
      None,
      requestFilter
    )

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAggregatorRoute[AddressingConfig](
      pathMatcher,
      aggregator,
      None,
      requestFilter,
      responseFilter
    )

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAggregatorRoute[AddressingConfig](
      pathMatcher,
      aggregator,
      Option(requiredPermission),
      NoOpRequestFilter,
      responseFilter
    )

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAggregatorRoute[AddressingConfig](
      pathMatcher,
      aggregator,
      Option(requiredPermission),
      requestFilter
    )

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAggregatorRoute[AddressingConfig](
      pathMatcher,
      aggregator,
      Option(requiredPermission),
      requestFilter,
      responseFilter
    )

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[AuthConfig#AuthData] = NoOpRequestFilter,
      responseFilter: ResponseFilter[AuthConfig#AuthData] = NoOpResponseFilter
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route = {
    pathPrefix(pathMatcher) {
      aggregatorRoute[AddressingConfig](
        aggregator,
        requiredPermission,
        requestFilter,
        responseFilter
      )
    }
  }

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: AuthConfig#Permission
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](aggregator, Option(requiredPermission))

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      Option(requiredPermission),
      requestFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      Option(requiredPermission),
      NoOpRequestFilter,
      responseFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](aggregator, None, requestFilter)

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      None,
      NoOpRequestFilter,
      responseFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      None,
      requestFilter,
      responseFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: AuthConfig#Permission,
      requestFilter: RequestFilter[AuthConfig#AuthData],
      responseFilter: ResponseFilter[AuthConfig#AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      Option(requiredPermission),
      requestFilter,
      responseFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[AuthConfig#AuthData, AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None,
      requestFilter: RequestFilter[AuthConfig#AuthData] = NoOpRequestFilter,
      responseFilter: ResponseFilter[AuthConfig#AuthData] = NoOpResponseFilter
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route = {
    extractRequest { request =>
      implicit val reqMeta = RequestMetadata.fromRequest(request)

      requireAuthentication(headerAuthConfig)(requiredPermission.asInstanceOf[Option[headerAuthConfig.Permission]])(
        reqMeta,
        ctx.metrics
      ) { authData =>
        filterRequest(requestFilter, authData) {
          addAuthHeader(headerAuthConfig)(Some(authData).asInstanceOf[Option[headerAuthConfig.AuthData]])(reqMeta) {
            filterResponse(responseFilter, authData) {
              extractRequest { authedRequest =>
                complete {
                  aggregator(
                    authedRequest,
                    AggregatorDependencies(headerAuthConfig, ctx.httpProxy, ctx.executionContext, ctx.materializer),
                    authData
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
