package com.pagerduty.arrivals.impl

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.api
import com.pagerduty.arrivals.api.RequestResponder
import com.pagerduty.arrivals.api.filter.{
  NoOpRequestFilter,
  NoOpResponseFilter,
  RequestFilter,
  ResponseFilter
}

import scala.concurrent.{ExecutionContext, Future}

trait RequestHandler[ResponderInput, RequestData]
    extends api.RequestHandler[ResponderInput, RequestData] {
  implicit def executionContext: ExecutionContext

  def apply(request: HttpRequest,
            responderInput: ResponderInput,
            requestData: RequestData,
            requestResponder: RequestResponder[ResponderInput, RequestData],
            requestFilter: RequestFilter[RequestData] = NoOpRequestFilter,
            responseFilter: ResponseFilter[RequestData] = NoOpResponseFilter)
    : Future[HttpResponse] = {
    requestFilter(request, requestData).flatMap {
      case Right(transformedRequest) =>
        requestResponder(transformedRequest, responderInput, requestData)
          .flatMap { response =>
            responseFilter(request, response, requestData)
          }
      case Left(response) =>
        Future.successful(response)
    }
  }

}
