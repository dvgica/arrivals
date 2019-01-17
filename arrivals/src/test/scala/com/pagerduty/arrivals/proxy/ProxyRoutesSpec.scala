package com.pagerduty.arrivals.proxy

import akka.http.scaladsl.model.ws.{Message, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import com.pagerduty.arrivals.ArrivalsContext
import com.pagerduty.arrivals.api.filter.{RequestFilter, ResponseFilter}
import com.pagerduty.arrivals.api.proxy.Upstream
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpecLike, Matchers}

import scala.concurrent.Future

class ProxyRoutesSpec extends FreeSpecLike with Matchers with ScalatestRouteTest with MockFactory { outer =>

  "ProxyRoutes" - {
    val expectedResponse = HttpResponse(201)

    implicit val ctx = ArrivalsContext(
      (),
      buildHttpClient = (_, _) =>
        new HttpClient {
          def executeRequest(request: HttpRequest): Future[HttpResponse] = Future.successful(expectedResponse)

          def executeWebSocketRequest[T](
              request: WebSocketRequest,
              clientFlow: Flow[Message, Message, T]
            ): (Future[WebSocketUpgradeResponse], T) = ???
      }
    )

    val upstream = new Upstream[Unit] {
      val metricsTag = "test"
      def addressRequest(request: HttpRequest, addressingConfig: Unit): HttpRequest = request
    }

    import ProxyRoutes._

    "filters and proxies routes" in {
      val requestTransformer = new RequestFilter[Any] {
        def apply(request: HttpRequest, t: Any): Future[Right[Nothing, HttpRequest]] = {
          Future.successful(Right(request.withUri("transformed")))
        }
      }

      val transformedResponse = HttpResponse(StatusCodes.MethodNotAllowed)

      val responseTransformer = new ResponseFilter[Any] {
        def apply(request: HttpRequest, response: HttpResponse, t: Any): Future[HttpResponse] = {
          response should equal(expectedResponse)

          Future.successful(transformedResponse)
        }
      }

      Seq(Get(_: String), Post(_: String), Put(_: String), Delete(_: String), Patch(_: String)).foreach { verb =>
        verb("/") ~> proxyRoute(upstream, requestTransformer, responseTransformer) ~> check {
          handled shouldBe true
          response should equal(transformedResponse)
        }
      }
    }
  }
}
