package com.pagerduty.akka.http.requestauthentication

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationConfig
import com.pagerduty.akka.http.requestauthentication.model.AuthenticationData.AuthFailedReason
import com.pagerduty.akka.http.support.{MetadataLogging, RequestMetadata}
import com.pagerduty.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object RequestAuthenticator {
  case object AuthenticationFutureFailed
      extends AuthFailedReason("authentication_future_exception")
  case object UnexpectedFailureWhileAuthenticating
      extends AuthFailedReason("unexpected_failure_while_authenticating")
  case object InvalidCredentialReason
      extends AuthFailedReason("invalid_credential")
}

trait RequestAuthenticator extends MetadataLogging {
  import RequestAuthenticator._

  implicit def executionContext: ExecutionContext
  def metrics: Metrics

  def authenticate(
      authConfig: AuthenticationConfig
  )(request: HttpRequest,
    stripAuthorizationHeader: Boolean,
    requiredPermission: Option[authConfig.Permission])(
      handler: (HttpRequest,
                Option[authConfig.AuthData]) => Future[HttpResponse])(
      implicit reqMeta: RequestMetadata): Future[HttpResponse] = {

    val extractedCredentials = authConfig.extractCredentials(request)

    val (authDataFuture, hasToken) = extractedCredentials match {
      case cred :: Nil =>
        // one credential was provided
        (attemptAuthentication(authConfig)(cred, request, requiredPermission),
         true)
      case _ :: _ =>
        // multiple creds were provided, we won't attempt authentication
        (Future.successful(None), true)
      case _ =>
        // no credentials were provided
        (Future.successful(None), false)
    }

    val strippedRequest =
      if (stripAuthorizationHeader && hasToken)
        request.mapHeaders(_.filterNot(_.is("authorization")))
      else
        request

    authDataFuture flatMap { optAuthData =>
      handler(strippedRequest, optAuthData)
    }
  }

  private def authenticationErrorHandler[AuthDataType](
      implicit reqMeta: RequestMetadata
  ): PartialFunction[Throwable, Option[AuthDataType]] = {
    case e =>
      emitAuthFailedMetric(AuthenticationFutureFailed)
      log.error(s"Authentication future failed with exception: $e")
      None
  }

  private def attemptAuthentication(
      authConfig: AuthenticationConfig
  )(cred: authConfig.Cred,
    request: HttpRequest,
    requiredPermission: Option[authConfig.Permission])(
      implicit reqMeta: RequestMetadata)
    : Future[Option[authConfig.AuthData]] = {
    authConfig
      .authenticate(cred)
      .map {
        case Success(Some(data)) =>
          authConfig.authDataGrantsPermission(data,
                                              request,
                                              requiredPermission) match {
            case Some(reason) =>
              emitAuthFailedMetric(reason)
              None
            case None => Some(data)
          }
        case Success(None) =>
          emitAuthFailedMetric(InvalidCredentialReason)
          None
        case Failure(_) =>
          emitAuthFailedMetric(UnexpectedFailureWhileAuthenticating)
          None
      }
      .recover(authenticationErrorHandler)
  }

  private def emitAuthFailedMetric(reason: AuthFailedReason): Unit = {
    metrics.increment("authentication_attempt_count",
                      ("result", "failure"),
                      ("reason", reason.metricTag))
  }
}
