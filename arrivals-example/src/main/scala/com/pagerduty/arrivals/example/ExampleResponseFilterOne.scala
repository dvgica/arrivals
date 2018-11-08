package com.pagerduty.arrivals.example

import akka.http.scaladsl.model._
import com.pagerduty.arrivals.api.filter.{SyncResponseFilter, SyncResponseFilterOutput}
import com.pagerduty.arrivals.impl.filter.ComposableResponseFilter

object ExampleResponseFilterOne extends SyncResponseFilter[Any] with ComposableResponseFilter[Any] {
  def applySync(request: HttpRequest, response: HttpResponse, data: Any): SyncResponseFilterOutput = {
    response match {
      case resp @ HttpResponse(StatusCodes.OK, _, HttpEntity.Strict(_, body), _) =>
        resp.withEntity(body.utf8String + "\n\nThis line was added in the first response filter")
      case resp @ _ => resp
    }
  }

}
