package com.pagerduty.arrivals.impl.filter

import akka.http.scaladsl.model.HttpRequest
import com.pagerduty.arrivals.api
import com.pagerduty.arrivals.api.filter.{RequestFilter, RequestFilterOutput}

import scala.concurrent.{ExecutionContext, Future}

trait CombinableRequestFilter[-RequestData] extends api.filter.RequestFilter[RequestData] { base =>

  def combine[U <: RequestData](filter: RequestFilter[U])(implicit ec: ExecutionContext): RequestFilter[U] = {
    new RequestFilter[U] {
      override def apply(request: HttpRequest, data: U): RequestFilterOutput = {
        base.apply(request, data) flatMap {
          case Right(interimRequest) => filter.apply(interimRequest, data)
          case Left(response)        => Future.successful(Left(response))
        }
      }
    }
  }
}
