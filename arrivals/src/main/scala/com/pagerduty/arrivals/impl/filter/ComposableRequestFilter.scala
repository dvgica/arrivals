package com.pagerduty.arrivals.impl.filter

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.arrivals.api
import com.pagerduty.arrivals.api.filter.{RequestFilter, RequestFilterOutput}

import scala.concurrent.{ExecutionContext, Future}

trait ComposableRequestFilter[-RequestData] extends api.filter.RequestFilter[RequestData] { base =>

  def combine[T <: RequestData](filter: RequestFilter[T])(implicit ec: ExecutionContext): RequestFilter[T] = {
    new RequestFilter[T] {
      override def apply(request: HttpRequest, data: T): RequestFilterOutput = {
        base.apply(request, data).flatMap {
          case Right(interimRequest) => filter.apply(interimRequest, data)
          case Left(response)        => Future.successful(Left(response))
        }
      }
    }
  }
}
