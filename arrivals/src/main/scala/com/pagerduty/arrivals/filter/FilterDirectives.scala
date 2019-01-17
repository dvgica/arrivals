package com.pagerduty.arrivals.filter

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.{Directive, Directive0, Directive1, RouteResult}
import com.pagerduty.arrivals.api.filter._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete

import scala.concurrent.Future

object FilterDirectives {

  def filterRequest[RequestData](requestFilter: RequestFilter[RequestData], requestData: RequestData): Directive0 = {
    extractRequestFilterResult(requestFilter, requestData).flatMap {
      case Right(filteredRequest) => mapRequest(_ => filteredRequest)
      case Left(response)         => complete(response)
    }
  }

  private def extractRequestFilterResult[RequestData](
      requestFilter: RequestFilter[RequestData],
      requestData: RequestData
    ): Directive1[Either[HttpResponse, HttpRequest]] = {
    Directive { inner => ctx =>
      implicit val ec = ctx.executionContext

      requestFilter(ctx.request, requestData).flatMap { result =>
        inner(Tuple1(result))(ctx)
      }
    }
  }

  def filterResponse[RequestData](responseFilter: ResponseFilter[RequestData], requestData: RequestData): Directive0 = {
    Directive { inner => ctx =>
      implicit val ec = ctx.executionContext

      inner(())(ctx).flatMap {
        case completeResult: Complete =>
          responseFilter(ctx.request, completeResult.response, requestData).map { filteredResponse =>
            RouteResult.Complete(filteredResponse)
          }
        case incompleteResult => Future.successful(incompleteResult)
      }
    }
  }

}
