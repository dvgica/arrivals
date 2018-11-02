package com.pagerduty.arrivals.impl

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import com.pagerduty.arrivals.api.RequestResponder
import com.pagerduty.arrivals.api.filter.{RequestFilter, RequestFilterOutput, ResponseFilter, ResponseFilterOutput}
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class RequestHandlerSpec extends FreeSpecLike with Matchers {
  "RequestHandler" - {
    val requestData = "some-req-data"
    val responderInput = 1234

    val handler = new RequestHandler[Int, String] {
      val executionContext = ExecutionContext.global
    }

    "applies request filters, applies the responder, then applies the response filters" in {
      val expectedResponse = HttpResponse(StatusCodes.AlreadyReported)

      val reqFilter = new RequestFilter[String] {
        def apply(request: HttpRequest, data: String): RequestFilterOutput =
          Future.successful(Right(request.withEntity(data)))
      }

      val respFilter = new ResponseFilter[String] {
        def apply(request: HttpRequest, response: HttpResponse, data: String): ResponseFilterOutput =
          Future.successful(expectedResponse)
      }

      val responder = new RequestResponder[Int, String] {
        def apply(request: HttpRequest, input: Int, data: String): Future[HttpResponse] = {
          if (input != responderInput)
            throw new RuntimeException("Responder did not receive expected input")
          if (data != requestData)
            throw new RuntimeException("Responder did not receive expected request data")
          if (request != HttpRequest().withEntity(requestData))
            throw new RuntimeException("Request was not filtered as expected")
          Future.successful(HttpResponse())
        }
      }

      val response =
        Await.result(handler(HttpRequest(), responderInput, requestData, responder, reqFilter, respFilter), 2.seconds)
      response should equal(expectedResponse)
    }

    "short-circuits the request handling if a request filter returns a response" in {
      val expectedResponse = HttpResponse(StatusCodes.BandwidthLimitExceeded)

      val reqFilter = new RequestFilter[String] {
        def apply(request: HttpRequest, data: String): RequestFilterOutput =
          Future.successful(Left(expectedResponse))
      }

      val respFilter = new ResponseFilter[String] {
        def apply(request: HttpRequest, response: HttpResponse, data: String): ResponseFilterOutput =
          throw new RuntimeException("Should not execute ResponseFilter")
      }

      val responder = new RequestResponder[Int, String] {
        def apply(request: HttpRequest, input: Int, data: String): Future[HttpResponse] = {
          throw new RuntimeException("Should not execute RequestResponder")
        }
      }

      val response =
        Await.result(handler(HttpRequest(), responderInput, requestData, responder, reqFilter, respFilter), 2.seconds)
      response should equal(expectedResponse)
    }
  }
}
