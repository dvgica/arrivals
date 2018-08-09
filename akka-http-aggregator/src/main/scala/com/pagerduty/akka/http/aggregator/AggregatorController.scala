package com.pagerduty.akka.http.aggregator

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatcher, Route}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.pagerduty.akka.http.headerauthentication.HeaderAuthenticator
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.aggregator.aggregator.Aggregator
import com.pagerduty.akka.http.proxy.HttpProxy
import com.pagerduty.akka.http.support.RequestMetadata

import scala.concurrent.{ExecutionContext, Future}

trait AggregatorController[AuthConfig <: HeaderAuthConfig, AddressingConfig] {
  val authConfig: AuthConfig
  def httpProxy: HttpProxy[AddressingConfig]
  def headerAuthenticator: HeaderAuthenticator
  implicit def executionContext: ExecutionContext
  implicit def materializer: Materializer

  def prefixAggregatorRoute[RequestKey, AccumulatedState](
      pathMatcher: PathMatcher[Unit],
      aggregator: Aggregator[RequestKey, AccumulatedState, AddressingConfig],
      requiredPermission: Option[AuthConfig#Permission] = None
  ): Route = {
    pathPrefix(pathMatcher) {
      aggregatorRoute(aggregator, requiredPermission)
    }
  }

  def aggregatorRoute[RequestKey, AccumulatedState](
      aggregator: Aggregator[RequestKey, AccumulatedState, AddressingConfig],
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
          // get the initial upstream requests from the aggregator, along with any saved state
          val (initialState, initialRequests) =
            aggregator.handleIncomingRequest(authConfig)(incomingRequest,
                                                         authData)

          // execute those initial upstream requests
          val futureInitialResponses = executeRequests(authConfig,
                                                       initialState,
                                                       initialRequests,
                                                       authedRequest)

          // we have the initial upstream responses, pass them into the first handler, and iterate through the subsequent handlers
          val finalStateAndResponseMap =
            aggregator.intermediateResponseHandlers.foldLeft(
              futureInitialResponses) { (futureStateAndResponses, builder) =>
              futureStateAndResponses.flatMap {
                case (previousState, responseMap) =>
                  // we have the responses, pass them to the next handler along with the previous state
                  val (newState, requests) =
                    builder(previousState, responseMap)

                  // execute the next stage of requests returned from the handler
                  executeRequests(authConfig,
                                  newState,
                                  requests,
                                  authedRequest)
              }
            }

          // all the intermediate handlers have run, tell the aggregator to build the outgoing response
          finalStateAndResponseMap.map {
            case (finalState, responseMap) =>
              aggregator.buildOutgoingResponse(finalState, responseMap)
          }
        }
      }
    }
  }

  private def executeRequests[RequestKey, AccumulatedState](
      authConfig: HeaderAuthConfig,
      state: AccumulatedState,
      requests: Map[RequestKey,
                    (AggregatorUpstream[AddressingConfig], HttpRequest)],
      authedRequest: HttpRequest
  ): Future[(AccumulatedState, Map[RequestKey, (HttpResponse, String)])] = {
    val preparedRequests = requests.map {
      case (key, (upstream, req)) =>
        val preppedReq =
          upstream.prepareAggregatorRequestForDelivery(authConfig,
                                                       req,
                                                       authedRequest)
        (key, (upstream, preppedReq))
    }

    Future
      .sequence(preparedRequests.map {
        case (key, (upstream, r)) =>
          httpProxy
            .request(r, upstream)
            .flatMap(resp => {
              Unmarshal(resp.entity)
                .to[String]
                .map(entity => (key, (resp, entity)))
            })
      }.toSeq)
      .map(_.toMap)
      .map(responseMap => (state, responseMap))
  }
}
