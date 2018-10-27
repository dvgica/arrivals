package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.proxy.responder.{
  RequestResponder,
  SimpleRequestResponder
}
import com.pagerduty.akka.http.proxy.filter.{
  NoOpRequestFilter,
  NoOpResponseFilter,
  RequestFilter,
  ResponseFilter
}

import scala.concurrent.{ExecutionContext, Future}

trait RequestHandler[T, RequestData] {
  implicit def executionContext: ExecutionContext

  def handle(request: HttpRequest,
             t: T,
             requestResponder: RequestResponder[T, RequestData],
             requestFilter: RequestFilter[RequestData] = NoOpRequestFilter,
             responseFilter: ResponseFilter[RequestData] = NoOpResponseFilter,
             requestData: Option[RequestData] = None): Future[HttpResponse] = {
    applyRequestFilter(request, requestFilter, requestData) {
      transformedRequest =>
        requestResponder(transformedRequest, t, requestData).flatMap {
          response =>
            responseFilter(request, response, requestData)
        }
    }
  }

  def handleSimple(
      request: HttpRequest,
      requestResponder: SimpleRequestResponder[RequestData],
      requestTransformer: RequestFilter[RequestData] = NoOpRequestFilter,
      responseTransformer: ResponseFilter[RequestData] = NoOpResponseFilter,
      requestData: Option[RequestData] = None): Future[HttpResponse] = {
    applyRequestFilter(request, requestTransformer, requestData) {
      transformedRequest =>
        requestResponder(transformedRequest, requestData).flatMap { response =>
          responseTransformer(request, response, requestData)
        }
    }
  }

  private def applyRequestFilter(request: HttpRequest,
                                 requestFilter: RequestFilter[RequestData],
                                 requestData: Option[RequestData] = None)(
      andThen: HttpRequest => Future[HttpResponse]): Future[HttpResponse] = {
    requestFilter(request, requestData).flatMap {
      case Right(transformedRequest) =>
        andThen(transformedRequest)
      case Left(response) =>
        Future.successful(response)
    }
  }
}
