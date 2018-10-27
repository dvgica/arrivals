package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

package object filter {
  type SyncRequestFilterOutput = Either[HttpResponse, HttpRequest]
  type SyncResponseFilterOutput = HttpResponse

  type RequestFilterOutput = Future[SyncRequestFilterOutput]
  type ResponseFilterOutput = Future[SyncResponseFilterOutput]
}
