package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.model.Uri.Authority

trait Upstream {
  def metricsTag: String
  def overrideEnvName: String = metricsTag.toUpperCase

  lazy val hostnameEnvOverride = s"${overrideEnvName}_HOST"
  lazy val hostnameOverride = sys.env.get(hostnameEnvOverride)

  lazy val portEnvOverride = s"${overrideEnvName}_PORT"
  lazy val portOverride = sys.env.get(portEnvOverride).map(_.toInt)

  lazy val hostPortOverride = hostnameOverride.flatMap { host =>
    portOverride.map { port =>
      (host, port)
    }
  }

  def addressRequestWithOverrides(request: HttpRequest,
                                  localHostname: String): HttpRequest = {
    addressRequest(request).getOrElse(addressRequest(request, localHostname))
  }

  private def addressRequest(request: HttpRequest): Option[HttpRequest] = {
    hostPortOverride.map {
      case (host, port) =>
        addressRequestWithHostPort(request, host, port)
    }
  }

  def addressRequest(request: HttpRequest, localHostname: String): HttpRequest

  def addressRequestWithHostPort(request: HttpRequest,
                                 host: String,
                                 port: Int): HttpRequest = {
    val authority = Authority(Uri.Host(host), port)
    val targetedUri = request.uri.withScheme("http").withAuthority(authority)

    request.withUri(targetedUri)
  }
}

trait LocalPortUpstream extends Upstream {
  def localPort: Int

  def addressRequest(request: HttpRequest,
                     localHostname: String): HttpRequest = {
    addressRequestWithHostPort(request, localHostname, localPort)
  }
}
