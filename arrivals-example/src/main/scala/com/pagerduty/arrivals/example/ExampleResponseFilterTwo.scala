package com.pagerduty.arrivals.example

import akka.http.scaladsl.model._
import com.pagerduty.arrivals.api.filter.SyncResponseFilter

object ExampleResponseFilterTwo extends SyncResponseFilter[Any] {
  def applySync(request: HttpRequest, response: HttpResponse, data: Any): HttpResponse = {
    response match {
      case resp @ HttpResponse(StatusCodes.OK, _, HttpEntity.Strict(_, body), _) =>
        resp.withEntity(body.utf8String + "\n\nThis line was added in the second response filter")
      case resp @ _ => resp
    }
  }

}
