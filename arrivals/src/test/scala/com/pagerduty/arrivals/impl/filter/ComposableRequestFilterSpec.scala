package com.pagerduty.arrivals.impl.filter

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import com.pagerduty.arrivals.api.filter.{RequestFilter, RequestFilterOutput}
import org.scalatest.{AsyncFreeSpecLike, FreeSpecLike, Matchers}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

class ComposableRequestFilterSpec extends FreeSpecLike with Matchers {

  implicit val ec = ExecutionContext.global

  val uri = Uri("/abc")
  val firstHeaderName = "X-TEST-FirstFilter"
  val secondHeaderName = "X-TEST-SecondFilter"

  object FirstFilter extends RequestFilter[String] with ComposableRequestFilter[String] {
    def apply(request: HttpRequest, data: String): RequestFilterOutput = {
      Future.successful(Right(request.addHeader(RawHeader(firstHeaderName, data))))
    }
  }

  "ComposableRequestFilter" - {
    "can be composed with another RequestFilter of the same RequestData type" in {

      object SecondFilter extends RequestFilter[String] {
        def apply(request: HttpRequest, data: String): RequestFilterOutput = {
          Future.successful(Right(request.addHeader(RawHeader(secondHeaderName, data))))
        }
      }

      val composedFilter = FirstFilter.combine(SecondFilter)

      val filterResult = Await.result(composedFilter.apply(HttpRequest(HttpMethods.GET, uri), "test"), 5.seconds)

      filterResult match {
        case Right(req) => {
          req.headers.find(_.is(firstHeaderName.toLowerCase)) shouldEqual Some(RawHeader(firstHeaderName, "test"))
          req.headers.find(_.is(secondHeaderName.toLowerCase)) shouldEqual Some(RawHeader(secondHeaderName, "test"))
        }
        case Left(_) => fail("Whoopsie!")
      }
    }
  }

  "can be composed with another RequestFilter of different RequestData type" in {

    object SecondFilter extends RequestFilter[Any] {
      def apply(request: HttpRequest, data: Any): RequestFilterOutput = {
        Future.successful(Right(request.addHeader(RawHeader(secondHeaderName, "test"))))
      }
    }

    val composedFilter = FirstFilter.combine(SecondFilter)

    val filterResult = Await.result(composedFilter.apply(HttpRequest(HttpMethods.GET, uri), "test"), 5.seconds)

    filterResult match {
      case Right(req) => {
        req.headers.find(_.is(firstHeaderName.toLowerCase)) shouldEqual Some(RawHeader(firstHeaderName, "test"))
        req.headers.find(_.is(secondHeaderName.toLowerCase)) shouldEqual Some(RawHeader(secondHeaderName, "test"))
      }
      case Left(_) => fail("Whoopsie!")
    }
  }
}
