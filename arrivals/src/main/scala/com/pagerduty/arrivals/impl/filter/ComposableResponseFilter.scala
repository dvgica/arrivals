package com.pagerduty.arrivals.impl.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.api
import com.pagerduty.arrivals.api.filter.{ResponseFilter, ResponseFilterOutput}

import scala.concurrent.ExecutionContext

trait ComposableResponseFilter[-RequestData] extends api.filter.ResponseFilter[RequestData] { base =>

  def combine[T <: RequestData](filter: ResponseFilter[T])(implicit ec: ExecutionContext): ResponseFilter[T] = {
    new ResponseFilter[T] {
      override def apply(request: HttpRequest, response: HttpResponse, data: T): ResponseFilterOutput = {
        base.apply(request, response, data) flatMap { resp =>
          filter.apply(request, resp, data)
        }
      }
    }
  }
}
