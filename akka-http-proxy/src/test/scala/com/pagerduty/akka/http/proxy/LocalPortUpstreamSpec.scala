package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.model.Uri.Authority
import org.scalatest.{FreeSpecLike, Matchers}

class LocalPortUpstreamSpec extends FreeSpecLike with Matchers {
  "LocalPortUpstream" - {
    "targets a request to the upstreams local port" in {
      val request = HttpRequest()
      val localHostname = "some-local-host"
      val targetPort = 4567
      val upstream = new LocalPortUpstream {
        val localPort = targetPort
        val metricsTag = "test"
      }

      val targetedReq = upstream.addressRequest(request, localHostname)

      val expectedAuthority =
        Authority(Uri.Host(localHostname), upstream.localPort)
      targetedReq.uri.authority should equal(expectedAuthority)
    }
  }

}
