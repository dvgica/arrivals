package com.pagerduty.arrivals.filter

import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{AuthorizationFailedRejection, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.pagerduty.arrivals.api.filter.{RequestFilter, ResponseFilter}
import akka.http.scaladsl.server.Directives._
import org.scalatest.{FreeSpec, Matchers}

import scala.concurrent.Future

class FilterDirectivesSpec extends FreeSpec with Matchers with ScalatestRouteTest {
  "filterRequest" - {
    "filters the request when the filter returns a new request" in {
      val filter = new RequestFilter[String] {
        def apply(request: HttpRequest, data: String): Future[Right[Nothing, HttpRequest]] = {
          Future.successful(Right(HttpRequest(HttpMethods.PUT, uri = data)))
        }
      }

      val reqData = "test"

      val route = FilterDirectives.filterRequest(filter, reqData) {
        extractRequest { request =>
          complete {
            request should equal(HttpRequest(HttpMethods.PUT, uri = reqData))
            HttpResponse(StatusCodes.Created).withEntity(request.uri.toString)
          }
        }
      }

      Get("/") ~> route ~> check {
        response.status should equal(StatusCodes.Created)
        responseAs[String] should equal(reqData)
      }
    }

    "completes the request when the filter returns a response" in {
      val filter = new RequestFilter[String] {
        def apply(request: HttpRequest, data: String): Future[Left[HttpResponse, Nothing]] = {
          Future.successful(Left(HttpResponse(StatusCodes.BadRequest)))
        }
      }

      val reqData = "test"

      val route = FilterDirectives.filterRequest(filter, reqData) {
        extractRequest { request =>
          complete {
            HttpResponse(StatusCodes.Created).withEntity(request.uri.toString)
          }
        }
      }

      Get("/") ~> route ~> check {
        response.status should equal(StatusCodes.BadRequest)
        responseAs[String] shouldNot equal(reqData)
      }
    }

    "returns a 500 if the filter throws an exception" in {
      val filter = new RequestFilter[String] {
        def apply(request: HttpRequest, data: String): Future[Either[HttpResponse, HttpRequest]] = {
          throw new RuntimeException("expected test exception")
        }
      }

      val reqData = "test"

      val route = FilterDirectives.filterRequest(filter, reqData) {
        extractRequest { request =>
          complete {
            HttpResponse(StatusCodes.Created).withEntity(request.uri.toString)
          }
        }
      }

      Get("/") ~> route ~> check {
        response.status should equal(StatusCodes.InternalServerError)
        responseAs[String] shouldNot equal(reqData)
      }
    }
  }

  "filterResponse" - {
    "filters the response if the route completes" in {
      val filter = new ResponseFilter[String] {
        def apply(request: HttpRequest, response: HttpResponse, data: String): Future[HttpResponse] = {
          Future.successful(HttpResponse(StatusCodes.Created).withEntity(data))
        }
      }

      val reqData = "test"
      val route = FilterDirectives.filterResponse(filter, reqData) {
        complete(HttpResponse())
      }

      Get("/") ~> route ~> check {
        response.status should equal(StatusCodes.Created)
        responseAs[String] should equal(reqData)
      }
    }

    "does not filter the response if the route rejects" in {
      val filter = new ResponseFilter[String] {
        def apply(request: HttpRequest, response: HttpResponse, data: String): Future[HttpResponse] = {
          Future.successful(HttpResponse(StatusCodes.Created).withEntity(data))
        }
      }

      val reqData = "test"
      val route = FilterDirectives.filterResponse(filter, reqData) {
        reject(AuthorizationFailedRejection)
      }

      Get("/") ~> Route.seal(route) ~> check {
        response.status should equal(StatusCodes.Forbidden)
        responseAs[String] shouldNot equal(reqData)
      }
    }

    "responds with 500 if the filter throws an exception" in {
      val filter = new ResponseFilter[String] {
        def apply(request: HttpRequest, response: HttpResponse, data: String): Future[HttpResponse] = {
          throw new RuntimeException("expected test exception")
        }
      }

      val reqData = "test"
      val route = FilterDirectives.filterResponse(filter, reqData) {
        complete(HttpResponse())
      }

      Get("/") ~> route ~> check {
        response.status should equal(StatusCodes.InternalServerError)
      }
    }
  }
}
