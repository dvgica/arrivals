package com.pagerduty.arrivals.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.api
import com.pagerduty.arrivals.api.filter.ResponseFilter

import scala.concurrent.{ExecutionContext, Future}

trait ComposableResponseFilter[-RequestData] extends api.filter.ResponseFilter[RequestData] { base =>

  def ~>[T <: RequestData](filter: ResponseFilter[T])(implicit ec: ExecutionContext): ComposableResponseFilter[T] = {
    new ComposableResponseFilter[T] {
      override def apply(request: HttpRequest, response: HttpResponse, data: T): Future[HttpResponse] = {
        base.apply(request, response, data).flatMap { resp =>
          filter.apply(request, resp, data)
        }
      }
    }
  }
}
