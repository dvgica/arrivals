package com.pagerduty.akka.http.aggregator.aggregator

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.pagerduty.akka.http.aggregator.AggregatorUpstream
import com.pagerduty.akka.http.headerauthentication.model.HeaderAuthConfig
import com.pagerduty.akka.http.proxy.HttpProxy

import scala.concurrent.{ExecutionContext, Future}

trait Aggregator[RequestKey, AccumulatedState, AddressingConfig] {
  type RequestMap =
    Map[RequestKey, (AggregatorUpstream[AddressingConfig], HttpRequest)]
  type ResponseMap = Map[RequestKey, (HttpResponse, String)]
  type HandlerInput = (AccumulatedState, ResponseMap)
  type AccumulatedStateAndRequests = (AccumulatedState, RequestMap)
  type HandlerResult = Either[HttpResponse, AccumulatedStateAndRequests]
  type ResponseHandler = HandlerInput => HandlerResult

  // implement these methods
  def handleIncomingRequest(
      authConfig: HeaderAuthConfig
  )(incomingRequest: HttpRequest, authData: authConfig.AuthData): HandlerResult

  def intermediateResponseHandlers: Seq[ResponseHandler]

  def buildOutgoingResponse(accumulatedState: AccumulatedState,
                            upstreamResponses: ResponseMap): HttpResponse

  // implementation
  def execute(authConfig: HeaderAuthConfig)(authedRequest: HttpRequest,
                                            authData: authConfig.AuthData)(
      implicit httpProxy: HttpProxy[AddressingConfig],
      executionContext: ExecutionContext,
      materializer: Materializer): Future[HttpResponse] = {
    val initialHandlerResult =
      handleIncomingRequest(authConfig)(authedRequest, authData)

    initialHandlerResult match {
      case Right((initialState, initialRequests)) =>
        // the initial handler returned some requests, execute them
        val fInitialResponses = executeRequests(authConfig)(initialState,
                                                            initialRequests,
                                                            authedRequest)

        val intermediateHandlersResult = executeIntermediateHandlers(
          authConfig)(authedRequest, fInitialResponses)

        // all the intermediate handlers have run (or short-circuited), tell the aggregator to build the outgoing response
        intermediateHandlersResult.map {
          case Right((finalState, responseMap)) =>
            // we don't have a response yet, have the aggregator do the final response build
            buildOutgoingResponse(finalState, responseMap)
          case Left(shortCircuit) =>
            // we already have the final (short-circuit) response
            shortCircuit
        }
      case Left(httpResponse) =>
        // the initial handler short-circuited and provided a response from the initial request, don't run intermediate handlers or final builder
        Future.successful(httpResponse)
    }
  }

  private def executeIntermediateHandlers(authConfig: HeaderAuthConfig)(
      authedRequest: HttpRequest,
      fInitialStateAndResponses: Future[(AccumulatedState, ResponseMap)])(
      implicit httpProxy: HttpProxy[AddressingConfig],
      executionContext: ExecutionContext,
      materializer: Materializer)
    : Future[Either[HttpResponse, (AccumulatedState, ResponseMap)]] = {
    val fFirstIntermediateHandlerInput
      : Future[Either[HttpResponse, HandlerInput]] =
      fInitialStateAndResponses.map { initialStateAndResponses =>
        Right(initialStateAndResponses)
      }

    // we have the initial handler input; pass it into the first handler, and iterate through the subsequent handlers
    intermediateResponseHandlers.foldLeft(fFirstIntermediateHandlerInput) {
      (fRespOrStateAndResponses, handler) =>
        fRespOrStateAndResponses.flatMap {
          case Right((previousState, responseMap)) =>
            // we have state and responses, so we pass them to this handler
            val respOrNewRequestMap = handler(previousState, responseMap)

            respOrNewRequestMap match {
              case Right((newState, requests)) =>
                // the handler returned some requests, execute them
                executeRequests(authConfig)(newState, requests, authedRequest)
                  .map(responses => Right(responses))
              case Left(sc) =>
                // the handler returned a short-circuit response, wrap it
                Future.successful(Left(sc))
            }
          case shortCircuit @ Left(_) =>
            // we have a short-circuit response, don't execute this handler
            Future.successful(shortCircuit)
        }
    }
  }

  private def executeRequests[RequestKey, AccumulatedState](
      authConfig: HeaderAuthConfig)(
      state: AccumulatedState,
      requests: RequestMap,
      authedRequest: HttpRequest
  )(implicit httpProxy: HttpProxy[AddressingConfig],
    executionContext: ExecutionContext,
    materializer: Materializer): Future[(AccumulatedState, ResponseMap)] = {
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
