package com.pagerduty.akka.http.proxy

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.mapRequest

object ApiPathDirectives {
  val ApiV1PathRegex = "^/api/v1"
  val ApiV1Path = "/api/v1"
  val ApiV2Path = "/api/v2"
}

trait ApiPathDirectives {
  import ApiPathDirectives._

  def mapApiV1ToV2Path: Directive0 = mapRequest { req =>
    val newPath = req.uri.path.toString.replaceFirst(ApiV1PathRegex, ApiV2Path)
    requestWithPath(req, newPath)
  }

  def prependApiV2Path: Directive0 = prependApiPath(ApiV2Path)

  def prependApiV1Path: Directive0 = prependApiPath(ApiV1Path)

  private def prependApiPath(path: String): Directive0 = mapRequest { req =>
    val newPath = path + req.uri.path.toString
    requestWithPath(req, newPath)
  }

  private def requestWithPath(request: HttpRequest,
                              path: String): HttpRequest = {
    val newPath = Path(path)
    val newUri = request.uri.copy(path = newPath)
    request.withUri(newUri)
  }
}
