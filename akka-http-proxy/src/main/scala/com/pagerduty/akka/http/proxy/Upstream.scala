package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.model.Uri.Authority

trait Upstream[AddressingConfig] {
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

  def addressRequestWithOverrides(
      request: HttpRequest,
      addressingConfig: AddressingConfig): HttpRequest = {
    addressRequest(request).getOrElse(
      addressRequest(request, addressingConfig))
  }

  private def addressRequest(request: HttpRequest): Option[HttpRequest] = {
    hostPortOverride.map {
      case (host, port) =>
        addressRequestWithHostPort(request, host, port)
    }
  }

  def addressRequest(request: HttpRequest,
                     addressingConfig: AddressingConfig): HttpRequest

  def addressRequestWithHostPort(request: HttpRequest,
                                 host: String,
                                 port: Int): HttpRequest = {
    val authority = Authority(Uri.Host(host), port)
    val targetedUri = request.uri.withScheme("http").withAuthority(authority)

    request.withUri(targetedUri)
  }

  def prepareRequestForDelivery(request: HttpRequest): HttpRequest = request
}

trait CommonHostnameUpstream extends Upstream[String] {
  def port: Int

  def addressRequest(request: HttpRequest,
                     commonHostname: String): HttpRequest = {
    addressRequestWithHostPort(request, commonHostname, port)
  }
}
