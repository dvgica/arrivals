package com.pagerduty.arrivals.impl.authproxy

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebSocket, WebSocketRequest}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.github.tomakehurst.wiremock.http.Fault
import com.pagerduty.arrivals.impl.authproxy.support.{IntegrationSpec, TestAuthConfig}
import scalaj.http.{Http => HttpClient}

import scala.concurrent.Await
import scala.concurrent.duration._

class AuthProxyBehaviourSpec extends IntegrationSpec {
  import com.github.tomakehurst.wiremock.client.WireMock._

  "An HttpServer" - {
    "handles a proxied route" - {
      val path = "/api/v1/incidents/foo"

      "when the authenticated proxy request succeeds, no permissions required" in {
        val incomingHeaderKey = "X-Incoming-Header"
        val incomingHeaderValue = "incoming-value"

        val expectedStatus = 201
        val expectedResponse = "mocked service response"
        val expectedHeaderKey = "X-Test-Header"
        val expectedHeaderValue = "test-value"
        val expectedJson = "{\"foo\":\"bar\"}"
        val expectedContentType = "application/json"

        mockService.stubFor(
          post(urlEqualTo(path))
            .withHeader(incomingHeaderKey, equalTo(incomingHeaderValue))
            .withHeader("content-type", equalTo(expectedContentType))
            .withHeader(TestAuthConfig.authHeader.lowercaseName, equalTo(TestAuthConfig.authHeader.value))
            .withRequestBody(equalToJson(expectedJson))
            .willReturn(
              aResponse()
                .withStatus(expectedStatus)
                .withHeader(expectedHeaderKey, expectedHeaderValue)
                .withBody(expectedResponse)
            )
        )

        val r = HttpClient(url(path))
          .header(incomingHeaderKey, incomingHeaderValue)
          .header("Authorization", "Bearer GOODTOKEN")
          .postData(expectedJson)
          .header("content-type", expectedContentType)
          .asString
        r.code should equal(expectedStatus)
        r.body should include(expectedResponse)
        r.header(expectedHeaderKey).get should equal(expectedHeaderValue)

        mockService.verify(postRequestedFor(urlEqualTo(path)))
      }

      "when the authenticated proxy request succeeds, permissions required and present" in {
        val v2IncidentsPath = "/api/v2/incidents/foo"

        val incomingHeaderKey = "X-Incoming-Header"
        val incomingHeaderValue = "incoming-value"

        val expectedStatus = 201
        val expectedResponse = "mocked service response"
        val expectedHeaderKey = "X-Test-Header"
        val expectedHeaderValue = "test-value"
        val expectedJson = "{\"foo\":\"bar\"}"
        val expectedContentType = "application/json"

        mockService.stubFor(
          post(urlEqualTo(v2IncidentsPath))
            .withHeader(incomingHeaderKey, equalTo(incomingHeaderValue))
            .withHeader("content-type", equalTo(expectedContentType))
            .withHeader(TestAuthConfig.authHeader.lowercaseName, equalTo(TestAuthConfig.authHeader.value))
            .withRequestBody(equalToJson(expectedJson))
            .willReturn(
              aResponse()
                .withStatus(expectedStatus)
                .withHeader(expectedHeaderKey, expectedHeaderValue)
                .withBody(expectedResponse)
            )
        )

        val r = HttpClient(url(v2IncidentsPath))
          .header(incomingHeaderKey, incomingHeaderValue)
          .header("Authorization", "Bearer GOODTOKEN")
          .postData(expectedJson)
          .header("content-type", expectedContentType)
          .asString
        r.code should equal(expectedStatus)
        r.body should include(expectedResponse)
        r.header(expectedHeaderKey).get should equal(expectedHeaderValue)

        mockService.verify(postRequestedFor(urlEqualTo(v2IncidentsPath)))
      }

      "when the authenticated proxy request succeeds, permissions required and not present" in {
        val v2SchedulesPath = "/api/v2/schedules/foo"

        val incomingHeaderKey = "X-Incoming-Header"
        val incomingHeaderValue = "incoming-value"

        val expectedStatus = 201
        val expectedResponse = "mocked service response"
        val expectedHeaderKey = "X-Test-Header"
        val expectedHeaderValue = "test-value"
        val expectedJson = "{\"foo\":\"bar\"}"
        val expectedContentType = "application/json"

        mockService.stubFor(
          post(urlEqualTo(v2SchedulesPath))
            .withHeader(incomingHeaderKey, equalTo(incomingHeaderValue))
            .withHeader("content-type", equalTo(expectedContentType))
            .withRequestBody(equalToJson(expectedJson))
            .willReturn(
              aResponse()
                .withStatus(expectedStatus)
                .withHeader(expectedHeaderKey, expectedHeaderValue)
                .withBody(expectedResponse)
            )
        )

        val r = HttpClient(url(v2SchedulesPath))
          .header(incomingHeaderKey, incomingHeaderValue)
          .header("Authorization", "Bearer GOODTOKEN")
          .postData(expectedJson)
          .header("content-type", expectedContentType)
          .asString
        r.code should equal(expectedStatus)
        r.body should include(expectedResponse)
        r.header(expectedHeaderKey).get should equal(expectedHeaderValue)

        mockService.verify(
          postRequestedFor(urlEqualTo(v2SchedulesPath))
            .withoutHeader(TestAuthConfig.authHeader.lowercaseName)
        )
      }

      "when the non-authenticated proxy request succeeds, with all the fixins" in {
        val incomingHeaderKey = "X-Incoming-Header"
        val incomingHeaderValue = "incoming-value"

        val expectedStatus = 201
        val expectedResponse = "mocked service response"
        val expectedHeaderKey = "X-Test-Header"
        val expectedHeaderValue = "test-value"
        val expectedJson = "{\"foo\":\"bar\"}"
        val expectedContentType = "application/json"

        mockService.stubFor(
          post(urlEqualTo(path))
            .withHeader(incomingHeaderKey, equalTo(incomingHeaderValue))
            .withHeader("content-type", equalTo(expectedContentType))
            .withRequestBody(equalToJson(expectedJson))
            .willReturn(
              aResponse()
                .withStatus(expectedStatus)
                .withHeader(expectedHeaderKey, expectedHeaderValue)
                .withBody(expectedResponse)
            )
        )

        val r = HttpClient(url(path))
          .header(incomingHeaderKey, incomingHeaderValue)
          .header("Authorization", "Bearer BADTOKEN")
          .postData(expectedJson)
          .header("content-type", expectedContentType)
          .asString
        r.code should equal(expectedStatus)
        r.body should include(expectedResponse)
        r.header(expectedHeaderKey).get should equal(expectedHeaderValue)

        mockService.verify(
          postRequestedFor(urlEqualTo(path))
            .withoutHeader(TestAuthConfig.authHeader.lowercaseName)
        )
      }

      "when the proxy request returns a client error" in {
        val expectedStatus = 404

        mockService.stubFor(
          get(urlEqualTo(path))
            .willReturn(
              aResponse()
                .withStatus(expectedStatus)
            )
        )

        val r = HttpClient(url(path)).asString
        r.code should equal(expectedStatus)
      }

      "when the proxy request returns a server error" in {
        val expectedStatus = 500

        mockService.stubFor(
          get(urlEqualTo(path))
            .willReturn(
              aResponse()
                .withStatus(expectedStatus)
            )
        )

        val r = HttpClient(url(path)).asString
        r.code should equal(expectedStatus)
      }

      "when the proxy request returns garbage" in {
        val expectedStatus = 502

        mockService.stubFor(
          get(urlEqualTo(s"$path/garbage"))
            .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE))
        )

        val r = HttpClient(url(s"$path/garbage")).asString
        r.code should equal(expectedStatus)
      }
    }

    "proxies WebSockets" in {
      // fake upstream WebSocket server, based on https://github.com/akka/akka-http/blob/v10.1.5/docs/src/test/scala/docs/http/scaladsl/server/WebSocketExampleSpec.scala
      val stubWebSocketService =
        Flow[Message]
          .mapConcat {
            case tm: TextMessage.Strict =>
              TextMessage.Strict(s"Hello ${tm.text}") :: Nil
            case _ =>
              throw new RuntimeException("Unexpected message received in test server")
          }

      val requestHandler: HttpRequest => HttpResponse = {
        case req @ HttpRequest(HttpMethods.GET, Uri.Path("/ws"), _, _, _) =>
          req.header[UpgradeToWebSocket] match {
            case Some(upgrade) =>
              upgrade.handleMessages(stubWebSocketService)
            case None =>
              HttpResponse(400, entity = "Not a valid websocket request!")
          }
        case r: HttpRequest =>
          r.discardEntityBytes() // important to drain incoming HTTP Entity stream
          HttpResponse(404, entity = "Unknown resource!")
      }

      val bindingFuture =
        Http().bindAndHandleSync(requestHandler, interface = "localhost", port = 10100)

      // a simple WebSocket client
      val sink = Sink.head[Message]

      val sendSource = Source.single(TextMessage("test"))

      val flow = Flow.fromSinkAndSourceMat(sink, sendSource)(Keep.left)

      // send a WebSocket request to our server under test
      val resp = Http().singleWebSocketRequest(WebSocketRequest("ws://localhost:1234/ws"), flow)

      // wait for the response message and make sure it matches what the stub upstream should give
      val receivedMessage = Await.result(resp._2, 1.second)
      receivedMessage match {
        case tm: TextMessage.Strict => tm.text should equal("Hello test")
        case _ =>
          throw new RuntimeException("Received unexpected message in test")
      }

      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
    }
  }
}
