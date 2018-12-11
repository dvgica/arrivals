package com.pagerduty.arrivals.impl.auth

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directive, Directive1}
import akka.http.scaladsl.server.Directives._
import com.pagerduty.akka.http.support.{MetadataLogging, RequestMetadata}
import com.pagerduty.arrivals.api.auth.{AuthFailedReason, AuthenticationConfig}
import com.pagerduty.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object AuthenticationDirectives extends MetadataLogging {
  case object AuthenticationFutureFailed extends AuthFailedReason("authentication_future_exception")
  case object UnexpectedFailureWhileAuthenticating extends AuthFailedReason("unexpected_failure_while_authenticating")
  case object InvalidCredentialReason extends AuthFailedReason("invalid_credential")

  def requireAuthentication(
      authConfig: AuthenticationConfig
    )(requiredPermission: Option[authConfig.Permission]
    )(implicit reqMeta: RequestMetadata,
      metrics: Metrics
    ): Directive1[authConfig.AuthData] = {
    authenticate(authConfig)(requiredPermission).flatMap {
      case Some(authData) => provide(authData)
      case None           => complete(HttpResponse(StatusCodes.Forbidden))
    }
  }

  def authenticate(
      authConfig: AuthenticationConfig
    )(requiredPermission: Option[authConfig.Permission]
    )(implicit reqMeta: RequestMetadata,
      metrics: Metrics
    ): Directive1[Option[authConfig.AuthData]] =
    tryAuthenticate(authConfig)(requiredPermission).map {
      case Success(aD) => aD
      case Failure(e)  => None
    }

  private def tryAuthenticate(
      authConfig: AuthenticationConfig
    )(requiredPermission: Option[authConfig.Permission]
    )(implicit reqMeta: RequestMetadata,
      metrics: Metrics
    ): Directive1[Try[Option[authConfig.AuthData]]] =
    Directive { inner => ctx =>
      implicit val ec = ctx.executionContext
      authenticateRequest(authConfig)(requiredPermission, ctx.request).transformWith(t => inner(Tuple1(t))(ctx))
    }

  private def authenticateRequest(
      authConfig: AuthenticationConfig
    )(requiredPermission: Option[authConfig.Permission],
      request: HttpRequest
    )(implicit reqMeta: RequestMetadata,
      metrics: Metrics,
      executionContext: ExecutionContext
    ): Future[Option[authConfig.AuthData]] = {
    val extractedCredentials = authConfig.extractCredentials(request)

    extractedCredentials match {
      case cred :: Nil =>
        // one credential was provided
        attemptAuthentication(authConfig)(cred, request, requiredPermission)
      case _ :: _ =>
        // multiple creds were provided, we won't attempt authentication
        Future.successful(None)
      case _ =>
        // no credentials were provided
        Future.successful(None)
    }
  }

  private def attemptAuthentication(
      authConfig: AuthenticationConfig
    )(cred: authConfig.Cred,
      request: HttpRequest,
      requiredPermission: Option[authConfig.Permission]
    )(implicit reqMeta: RequestMetadata,
      metrics: Metrics,
      executionContext: ExecutionContext
    ): Future[Option[authConfig.AuthData]] = {
    authConfig
      .authenticate(cred)
      .map {
        case Success(Some(data)) =>
          authConfig.authDataGrantsPermission(data, request, requiredPermission) match {
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

  private def authenticationErrorHandler[AuthDataType](
      implicit reqMeta: RequestMetadata,
      metrics: Metrics
    ): PartialFunction[Throwable, Option[AuthDataType]] = {
    case e =>
      emitAuthFailedMetric(AuthenticationFutureFailed)
      log.error(s"Authentication future failed with exception: $e")
      None
  }

  private def emitAuthFailedMetric(reason: AuthFailedReason)(implicit metrics: Metrics): Unit = {
    metrics.increment("authentication_attempt_count", ("result", "failure"), ("reason", reason.metricTag))
  }
}
