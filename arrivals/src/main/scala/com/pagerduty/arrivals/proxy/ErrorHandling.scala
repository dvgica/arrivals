package com.pagerduty.arrivals.proxy

import akka.http.scaladsl.model.{HttpResponse, IllegalResponseException, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractRequest}
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.stream.StreamTcpException
import com.pagerduty.akka.http.support.{MetadataLogging, RequestMetadata}
import com.pagerduty.metrics.Metrics

/** Useful error handling for wrapping an API Gateway's routes. */
trait ErrorHandling extends MetadataLogging {

  def metrics: Metrics

  lazy val proxyExceptionHandler = ExceptionHandler {
    case e: IllegalResponseException =>
      handleProxyError(e)
    case e: java.lang.RuntimeException if e.getClass.getSimpleName.contains("UnexpectedConnectionClosureException") =>
      handleProxyError(e)
    case e: StreamTcpException if e.getMessage.contains("Connection refused") =>
      handleProxyError(e)
  }

  private def handleProxyError(t: Throwable): Route = {
    metrics.increment("proxy_error", ("exception", t.getClass.getSimpleName))
    extractRequest { req =>
      implicit val logCtx = RequestMetadata.fromRequest(req)
      log.error(s"Error proxying request: $t")
      complete(HttpResponse(status = StatusCodes.BadGateway))
    }
  }
}
