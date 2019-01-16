package com.pagerduty.arrivals.api.auth

/** A reason authentication (as defined in [[AuthenticationConfig.authenticate]]) failed. */
trait AuthFailedReason {

  /** Define the tag which will be appended to the authentication failure metric. */
  def metricTag: String
}
