package com.pagerduty.arrivals.auth

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{Directive, Directive1}
import akka.http.scaladsl.server.Directives._
import com.pagerduty.akka.http.support.{MetadataLogging, RequestMetadata}
import com.pagerduty.arrivals.api.auth.{AuthFailedReason, AuthenticationConfig}
import com.pagerduty.metrics.Metrics

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/** These directives authenticate the credential included on a request, extracting the associated authentication data.
  *
  * They can be used independently of the top-level `Route`s defined by Arrivals if more control is desired.  *
  */
object AuthenticationDirectives extends MetadataLogging {
  case object AuthenticationFutureFailed extends AuthFailedReason { val metricTag = "authentication_future_exception" }
  case object UnexpectedFailureWhileAuthenticating extends AuthFailedReason {
    val metricTag = "unexpected_failure_while_authenticating"
  }
  case object InvalidCredentialReason extends AuthFailedReason { val metricTag = "invalid_credential" }

  /** Require a request to have an authenticated credential, otherwise respond with 403 Forbidden.
    *
    * @param authConfig
    * @param requiredPermission
    * @param reqMeta
    * @param metrics
    * @return A `Directive` extracting `AuthData`
    */
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

  /** Authenticate a request's credential, but allow it to proceed regardless.
    *
    * Currently, if multiple credentials are present on the request, the request is not considered to be authenticated.
    * Also, there is a mix of authentication and authorization here which isn't great.
    *
    * @param authConfig
    * @param requiredPermission
    * @param reqMeta
    * @param metrics
    * @return A `Directive` extracting `Option[AuthData]`
    */
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
      attemptAuthentication(authConfig)(ctx.request, requiredPermission).transformWith(t => inner(Tuple1(t))(ctx))
    }

  private def attemptAuthentication(
      authConfig: AuthenticationConfig
    )(request: HttpRequest,
      requiredPermission: Option[authConfig.Permission]
    )(implicit reqMeta: RequestMetadata,
      metrics: Metrics,
      executionContext: ExecutionContext
    ): Future[Option[authConfig.AuthData]] = {
    authConfig
      .authenticate(request)
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
