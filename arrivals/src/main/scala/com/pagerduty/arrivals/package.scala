package com.pagerduty

/** Arrivals is an open-source project providing building blocks for constructing an API Gateway using Akka HTTP.
  * [[https://www.pagerduty.com PagerDuty]] has [[https://www.youtube.com/watch?v=DRxLFWmvJ8A used it]] in production to build a few
  * [[https://samnewman.io/patterns/architectural/bff/ BFFs]]. Currently, the project is focused on the following areas:
  *
  *  - Proxying HTTP requests to upstream services
  *  - Authenticating those requests
  *  - Adding a header to prove authentication to upstream services
  *  - Filtering and transforming requests and responses
  *  - Turning a single request into multiple upstream requests, and aggregating the responses into a single response
  *
  * As much as possible, Arrivals follows idioms found in Akka HTTP. This means that it exposes `Route`s and `Directive`s
  * to the library user which can be combined with other Akka HTTP-based code.
  *
  * For installation and usage, please see [[https://github.com/PagerDuty/arrivals]].
  *
  * Here's some API docs to try:
  *
  * - [[com.pagerduty.arrivals.authproxy.AuthProxyRoutes]]
  * - [[com.pagerduty.arrivals.aggregator.AggregatorRoutes]]
  */
package object arrivals {}
