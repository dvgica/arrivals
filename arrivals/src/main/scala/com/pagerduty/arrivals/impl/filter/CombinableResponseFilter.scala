package com.pagerduty.arrivals.impl.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.api
import com.pagerduty.arrivals.api.filter.{ResponseFilter, ResponseFilterOutput}

import scala.concurrent.ExecutionContext

trait CombinableResponseFilter[-RequestData] extends api.filter.ResponseFilter[RequestData] { base =>

  def combine[U <: RequestData](filter: ResponseFilter[U])(implicit ec: ExecutionContext): ResponseFilter[U] = {
    new ResponseFilter[U] {
      override def apply(request: HttpRequest, response: HttpResponse, data: U): ResponseFilterOutput = {
        base.apply(request, response, data) flatMap { resp =>
          filter.apply(request, resp, data)
        }
      }
    }
  }
}
