package com.pagerduty.akka.http.authproxy

import com.github.tomakehurst.wiremock.http.Fault
import com.pagerduty.akka.http.authproxy.support.{
  IntegrationSpec,
  TestAuthConfig
}
import scalaj.http.{Http => HttpClient}

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
            .withHeader(TestAuthConfig.authHeader.lowercaseName,
                        equalTo(TestAuthConfig.authHeader.value))
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
            .withHeader(TestAuthConfig.authHeader.lowercaseName,
                        equalTo(TestAuthConfig.authHeader.value))
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
            .withoutHeader(TestAuthConfig.authHeader.lowercaseName))
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
  }
}
