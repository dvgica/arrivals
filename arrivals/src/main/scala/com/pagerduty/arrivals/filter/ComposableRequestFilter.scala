package com.pagerduty.arrivals.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.arrivals.api
import com.pagerduty.arrivals.api.filter.RequestFilter

import scala.concurrent.{ExecutionContext, Future}

/** Allows for a `RequestFilter` to be composed or chained with another via the `~>` operator. */
trait ComposableRequestFilter[-RequestData] extends api.filter.RequestFilter[RequestData] { base =>

  def ~>[T <: RequestData](filter: RequestFilter[T])(implicit ec: ExecutionContext): ComposableRequestFilter[T] = {
    new ComposableRequestFilter[T] {
      override def apply(request: HttpRequest, data: T): Future[Either[HttpResponse, HttpRequest]] = {
        base.apply(request, data).flatMap {
          case Right(interimRequest) => filter.apply(interimRequest, data)
          case Left(response)        => Future.successful(Left(response))
        }
      }
    }
  }
}
