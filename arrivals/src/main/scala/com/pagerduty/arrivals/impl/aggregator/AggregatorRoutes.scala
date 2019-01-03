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

class AggregatorRoutes[AuthConfig <: HeaderAuthConfig](val headerAuthConfig: AuthConfig) {

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: headerAuthConfig.Permission
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    prefixAggregatorRoute[AddressingConfig](
      pathMatcher,
      aggregator,
      Option(requiredPermission)
    )

  def prefixAggregatorRoute[AddressingConfig](
      pathMatcher: PathMatcher[Unit],
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      responseFilter: ResponseFilter[headerAuthConfig.AuthData]
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
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requestFilter: RequestFilter[headerAuthConfig.AuthData]
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
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requestFilter: RequestFilter[headerAuthConfig.AuthData],
      responseFilter: ResponseFilter[headerAuthConfig.AuthData]
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
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      responseFilter: ResponseFilter[headerAuthConfig.AuthData]
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
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      requestFilter: RequestFilter[headerAuthConfig.AuthData]
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
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      requestFilter: RequestFilter[headerAuthConfig.AuthData],
      responseFilter: ResponseFilter[headerAuthConfig.AuthData]
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
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: Option[headerAuthConfig.Permission] = None,
      requestFilter: RequestFilter[headerAuthConfig.AuthData] = NoOpRequestFilter,
      responseFilter: ResponseFilter[headerAuthConfig.AuthData] = NoOpResponseFilter
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
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: headerAuthConfig.Permission
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](aggregator, Option(requiredPermission))

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      requestFilter: RequestFilter[headerAuthConfig.AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      Option(requiredPermission),
      requestFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      responseFilter: ResponseFilter[headerAuthConfig.AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      Option(requiredPermission),
      NoOpRequestFilter,
      responseFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requestFilter: RequestFilter[headerAuthConfig.AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](aggregator, None, requestFilter)

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      responseFilter: ResponseFilter[headerAuthConfig.AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      None,
      NoOpRequestFilter,
      responseFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requestFilter: RequestFilter[headerAuthConfig.AuthData],
      responseFilter: ResponseFilter[headerAuthConfig.AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      None,
      requestFilter,
      responseFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: headerAuthConfig.Permission,
      requestFilter: RequestFilter[headerAuthConfig.AuthData],
      responseFilter: ResponseFilter[headerAuthConfig.AuthData]
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route =
    aggregatorRoute[AddressingConfig](
      aggregator,
      Option(requiredPermission),
      requestFilter,
      responseFilter
    )

  def aggregatorRoute[AddressingConfig](
      aggregator: RunnableAggregator[headerAuthConfig.AuthData, AddressingConfig],
      requiredPermission: Option[headerAuthConfig.Permission] = None,
      requestFilter: RequestFilter[headerAuthConfig.AuthData] = NoOpRequestFilter,
      responseFilter: ResponseFilter[headerAuthConfig.AuthData] = NoOpResponseFilter
    )(implicit ctx: ArrivalsContext[AddressingConfig]
    ): Route = {
    extractRequest { request =>
      implicit val reqMeta = RequestMetadata.fromRequest(request)

      requireAuthentication(headerAuthConfig)(requiredPermission)(
        reqMeta,
        ctx.metrics
      ) { authData =>
        filterRequest(requestFilter, authData) {
          addAuthHeader(headerAuthConfig)(Some(authData))(reqMeta) {
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
