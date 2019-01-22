# Arrivals [![CircleCI](https://circleci.com/gh/PagerDuty/arrivals.svg?style=svg)](https://circleci.com/gh/PagerDuty/arrivals)

This open-source project provides building blocks for constructing an API Gateway using Akka HTTP. [PagerDuty](https://www.pagerduty.com) has [used it](https://www.youtube.com/watch?v=DRxLFWmvJ8A) in production to build a few [BFFs](https://samnewman.io/patterns/architectural/bff/). Currently, the project is focused on the following areas:

 - Proxying HTTP requests to upstream services
 - Authenticating those requests
 - Adding a header to prove authentication to upstream services
 - Filtering and transforming requests and responses
 - Turning a single request into multiple upstream requests, and aggregating the responses into a single response
  
As much as possible, Arrivals follows idioms found in Akka HTTP. This means that it exposes `Route`s and `Directive`s to the library user which can be combined with other Akka HTTP-based code.  

- [Example Application](#example-application)
- [Usage](#usage)
  - [Installation](#installation)
  - [Introduction and Setup](#introduction-and-setup)
  - [Routes](#routes)
  - [Filters](#filters)
- [API Docs](#api-docs)
- [License](#license)
- [Contributing](#contributing)
- [Contact](#contact)
- [TODO](#todo)

## Example Application

For the impatient, an example API Gateway using Arrivals is available in [arrivals-example](https://github.com/PagerDuty/arrivals/blob/master/arrivals-example/src/main/scala/com/pagerduty/arrivals/example/ExampleApp.scala). The example can be run by cloning this repository and running `sbt arrivalsExample/run`. Try the following URLs:

- http://localhost:8080/cats
- http://localhost:8080/dogs
- http://localhost:8080/dogs?username=rex
- http://localhost:8080/all?username=rex

For more details on what's happening, keep reading.

## Usage

### Installation

All artifacts are available at the PagerDuty Bintray OSS repository.

Add the PD Bintray to your resolvers with the following:

```
resolvers += "bintray-pagerduty-oss-maven" at "https://dl.bintray.com/pagerduty/oss-maven"
```

#### arrivals

This is the implementation artifact on which applications should depend.

```
"com.pagerduty" %% "arrivals" % arrivalsVersion
```

#### arrivals-api

Authors of custom implementations (e.g. `Filter`s, `Upstream`s, and `Aggregator`s) which live in a library
 should depend on this artifact, which will hopefully change less frequently.

```
"com.pagerduty" %% "arrivals-api" % arrivalsVersion
```

### Introduction and Setup

Arrivals functionality is provided via Akka HTTP `Route`s available in various `object`s or `class`es. These `Route`s 
function like any other Akka HTTP route, meaning they can be composed with other `Route`s from Akka and served with the usual
call to `Http().bindAndHandle`.

Read more about the Akka Routing DSL [here](https://doc.akka.io/docs/akka-http/current/routing-dsl/index.html).

#### Initialize `ArrivalsContext`

All Arrivals routes have an `implicit` dependency on an `ArrivalsContext`. 

``` scala
implicit val system = ActorSystem()
implicit val mat = ActorMaterializer()

implicit val arrivalsCtx = ArrivalsContext("localhost") // "localhost" is the hostname for all upstreams in this example
```

You must provide an `AddressingConfig`, which is a piece of data used by the proxy to address requests to an
`Upstream`. For example, it might be the hostname of a load balancer obtained dynamically at runtime from a container scheduler. In the example above, the `AddressingConfig` is just the string `"localhost"`.

In the event that you do not require this data, you can pass `Unit`.

Because everything in Arrivals is Akka-based, you must implicitly provide the usual Akka `ActorSystem`
and `Materializer`. A `Metrics` provider is optional and defaults to a no-op metrics implementation.

#### Declare an `Upstream`

Arrivals requests need somewhere to be proxied. This is called an `Upstream`. Here's an example of a simple upstream
that lives on the host provided by `AddressingConfig` at a specific port (1234 in this case):

``` scala
case object FooService extends Upstream[String] {
  val metricsTag = "foo"
  def addressRequest(request: HttpRequest, addressingConfig: String): HttpRequest = {
    val newUri =
        request.uri
          .withAuthority(Authority(Uri.Host(addressingConfig), 1234))
          .withScheme("http")
    request.withUri(newUri)
  }
}
```

#### Declare a `Route`

Then, declare a `Route`. Here we use `prefixProxyRoute` (discussed further below):

``` scala
import com.pagerduty.arrivals.impl.proxy.ProxyRoutes._

val route = prefixProxyRoute("foos", FooService)
```

#### Start the Akka HTTP Server

Finally, start the Akka HTTP server as you normally would:

``` scala
val binding = Http().bindAndHandle(route, "0.0.0.0", 8080)
```

Your proxy server is now running. Keep reading to see what else Arrivals can do for you.

### Routes

#### Proxy Routes

The `ProxyRoutes` object provides routes to proxy requests to an `Upstream`. No authentication is done. These routes are:

 - `prefixProxyRoute` - proxy any request matching the given path prefix
 - `proxyRoute` - proxy all requests (this is usually nested inside other Akka HTTP directives to narrow the scope, or used as a deliberate catch-all at the end of a series of routes)

These methods are overloaded with various combinations of parameters related to [`Filter`](#filters)s.

#### Auth Proxy Routes

The `AuthProxyRoutes` class provides routes to proxy requests to an `Upstream`, optionally adding a custom header to any request
that is authenticated. **Requests are proxied regardless of whether authentication or authorization succeeded!** Upstream services
should always verify the authentication header (e.g. via cryptographic signing) for routes that require authentication.

`AuthProxyRoutes` have an additional dependency on a `HeaderAuthConfig` which describes how to authenticate requests, check permissions,
and add a custom header if the request passes authentication and authorization. This `HeaderAuthConfig` is provided as an argument to the
`AuthProxyRoutes` constructor. After construction, the routes can be imported in the typical Akka HTTP style:

``` scala
val headerAuthConfig = new HeaderAuthConfig { /* ... */ }
val authProxyRoutes = new AuthProxyRoutes(headerAuthConfig)

import authProxyRoutes._

val route = prefixAuthProxyRoute("bar", FooService)
```

Similar to `ProxyRoutes`, both `prefixAuthProxyRoute` and `authProxyRoute` methods are provided in various permutations to allow for [`Filter`](#filters)s.

#### Aggregator Routes

The `AggregatorRoutes` class provides routes fulfilled by `Aggregator`s. An `Aggregator` is an entity that, based on an incoming request,
executes multiple waves of user-defined upstream requests, and then allows the user to build a single response from the upstream responses.

`AggregatorRoutes`, like `AuthProxyRoutes`, has a dependency on `HeaderAuthConfig`:

``` scala
val headerAuthConfig = new HeaderAuthConfig { /* ... */ }
val aggregatorRoutes = new AggregatorRoutes(headerAuthConfig)

case object BazAggregator extends Aggregator { /* ... */ }

import aggregatorRoutes._

val route = prefixAggregatorRoute("baz", BazAggregator)
```

Similar to `ProxyRoutes` and `AuthProxyRoutes`, both `prefixAggregatorRoute` and `aggregatorRoute` methods are provided in various permutations to allow for [`Filter`](#filters)s.

For more details on how to construct an `Aggregator`, please see the [API docs](https://pagerduty.github.io/arrivals/api/com/pagerduty/arrivals/api/aggregator/Aggregator.html).

### Filters

Filters allow for user-defined changes to requests before they are proxied, or responses before they are returned to the client.

All filters are provided with `RequestData`, a user-defined type, but when used with the `Routes` defined
by Arrivals this type is set to something specific:

- `ProxyRoutes`: `Unit`
- `AuthProxyRoutes`: `Option[AuthData]`
- `AggregatorRoutes`: `AuthData`

`AuthData` is a user-defined type in `AuthenticationConfig`. Users wishing to pass arbitrary data to a `Filter` should use the lower-level `FilterDirectives`.

#### Request Filters

Request filters can either transform a request into a new one, or short-circuit the rest of the filter/proxy/aggregation steps and
immediately return a response.

``` scala
object RateLimitRequestFilter extends RequestFilter[Option[UserId]] {
  def apply(request: HttpRequest, optUserId: Option[UserId]): Future[Either[HttpResponse, HttpRequest]] = {
    optUserId match {
      case Some(uId) =>
        hasUserReachedRateLimit(uId).map { reachedLimit =>
          if (reachedLimit) {
            Left(HttpResponse(StatusCodes.EnhanceYourCalm))
          } else {
            Right(request.addHeader(RawHeader("X-Rate-Limit-Checked", "true")))      
          }
        }
      case None => 
        Future.successful(Left(HttpResponse(StatusCodes.Forbidden, "This rate-limited endpoint requires auth!")))
    }
  }
  
  private def hasUserReachedRateLimit(userId: UserId): Future[Boolean] = { /* ... */ }
}
```

#### Response Filters

Response filters simply transform the outgoing response. Unlike `RequestFilter`s, they are not able to complete the request or short-circuit following filters.

Any `Filter` that doesn't use its `RequestData` should specify `Any` for the type parameter.

``` scala
object AddCacheControl extends ResponseFilter[Any] {
  def apply(request: HttpRequest, response: HttpResponse, data: Any): Future[HttpResponse] = {
    Future.successful(response.addHeader(Cache-Control(no-store))
  }
}
```

#### Specialized/Simplified Filters

Sometimes, as above, a simpler filter signature will suffice. Various specializations of `RequestFilter` and `ResponseFilter`
exist in `com.pagerduty.arrivals.api.filter`, for example:

``` scala
object AddCacheControl extends SyncResponseFilter[Any] {
  def applySync(request: HttpRequest, response: HttpResponse, data: Any): HttpResponse = {
    response.addHeader(Cache-Control(no-store)
  }
}
```

#### Filter Composition/Chaining

Filters can be composed such that the output of one is fed to the input of the next. This is accomplished by mixing in the
`ComposableRequestFilter` or `ComposableResponseFitler` traits.

``` scala
object FilterOne extends SyncRequestFilter[Any] with ComposableRequestFilter[Any] { /* ... */ }

object FilterTwo extends RequestFilter[String] { /* ... */ }

import ExecutionContext.Implicits.global // don't just copy-paste this ExecutionContext please!
val newFilter: ComposableRequestFilter[String] = FilterOne ~> FilterTwo
```

An arbitrary number of filters may be composed.

## API Docs

See [pagerduty.github.io/arrivals/](https://pagerduty.github.io/arrivals/).

## License

Copyright 2019, PagerDuty, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Contributing

Contributions are welcome in the form of pull-requests based on the master branch.

We ask that your changes are consistently formatted as the rest of the code in this repository, and also that any changes are covered by unit tests.

## Contact

This library is maintained by the Core team at PagerDuty. Opening a GitHub issue is the best way to get in touch with us.

## TODO

- Metadata logging is inconsistently used because it's a PITA - would be nice to do something less ugly and not include `akka-http-support` in `arrivals-api`
- De-couple authentication and authorization
