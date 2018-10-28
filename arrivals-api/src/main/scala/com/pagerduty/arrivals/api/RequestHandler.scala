package com.pagerduty.arrivals.api

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.api.filter.{
  NoOpRequestFilter,
  NoOpResponseFilter,
  RequestFilter,
  ResponseFilter
}

import scala.concurrent.Future

trait RequestHandler[ResponderInput, RequestData] {

  def apply(request: HttpRequest,
            responderInput: ResponderInput,
            requestData: RequestData,
            requestResponder: RequestResponder[ResponderInput, RequestData],
            requestFilter: RequestFilter[RequestData] = NoOpRequestFilter,
            responseFilter: ResponseFilter[RequestData] = NoOpResponseFilter)
    : Future[HttpResponse]
}
