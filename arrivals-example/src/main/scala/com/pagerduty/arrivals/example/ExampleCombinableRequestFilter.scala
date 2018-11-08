package com.pagerduty.arrivals.example

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import com.pagerduty.arrivals.api.filter.{RequestFilter, RequestFilterOutput}
import com.pagerduty.arrivals.impl.filter.CombinableRequestFilter

import scala.concurrent.{ExecutionContext, Future}

object ExampleCombinedRequestFilter {

  implicit val ec = ExecutionContext.global

  object ExampleRequestFilterOne extends RequestFilter[Any] with CombinableRequestFilter[Any] {
    def apply(request: HttpRequest, data: Any): RequestFilterOutput = {
      Future.successful(Right(request.addHeader(RawHeader("X-Bird-Name", "header bird"))))
    }
  }

  object ExampleRequestFilterTwo extends RequestFilter[Any] {
    def apply(request: HttpRequest, data: Any): RequestFilterOutput = {
      Future.successful(Right(request))
    }
  }

}
