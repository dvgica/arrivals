# akka-http-api-gateway [![CircleCI](https://circleci.com/gh/PagerDuty/akka-http-api-gateway.svg?style=svg)](https://circleci.com/gh/PagerDuty/akka-http-api-gateway)

This open-source project is a collection of small libraries that can be used to build an API gateway (or perhaps a few [BFFs](https://samnewman.io/patterns/architectural/bff/), [as PagerDuty has done](https://www.youtube.com/watch?v=DRxLFWmvJ8A)). Currently, the project is focused on proxying authenticated requests to upstream services. All sub-projects depend on [Akka HTTP](https://doc.akka.io/docs/akka-http/current/).

The philosophy of this project is to keep the sub-projects small, modular, and independently testable. Therefore, some wiring-together is required. Think of the sub-projects as LEGO blocks.

## Sub-Projects

All artifacts are published with the `com.pagerduty` `groupId` and are available at the PagerDuty Bintray OSS repository.

Add the PD Bintray to your resolvers with the following:

```
resolvers += "bintray-pagerduty-oss-maven" at "https://dl.bintray.com/pagerduty/oss-maven"
```

### akka-http-proxy

A `ProxyController` (a `trait` providing a `Route`) for proxying un-authenticated requests to an `Upstream` via an `HttpProxy`.

_Depends On_: `akka-http` and PD's [`scala-akka-support`](https://github.com/PagerDuty/scala-akka-support), [`metrics-api`](https://github.com/PagerDuty/scala-metrics)\
_Artifact ID_: `akka-http-proxy`

### akka-http-request-authentication

`RequestAuthentication` and `RequireAuthentication`, traits that authenticate HTTP requests based on a library-user-supplied `AuthenticationConfig`.

_Depends On_: `akka-http` and PD's [`scala-akka-support`](https://github.com/PagerDuty/scala-akka-support), [`metrics-api`](https://github.com/PagerDuty/scala-metrics)\
_Artifact ID_: `akka-http-request-authentication`

### akka-http-header-authentication

A `HeaderAuthenticator` trait which adds a library-user-supplied HTTP header (via `HeaderAuthConfig`) to authenticated requests. For example, this header might be a cryptographically signed header with data about the authenticated user, trusted by an upstream service.

_Depends On_: `akka-http-request-authentication`\
_Artifact ID_: `akka-http-header-authentication`

### akka-http-auth-proxy 

An `AuthProxyController` `trait` providing a `Route` which authenticates and proxies requests to an `Upstream`, adding a library-user-supplied HTTP header if authentication was successful.

_Depends On_: `akka-http-header-authentication`, `akka-http-proxy`\
_Artifact ID_: `akka-http-auth-proxy`

### akka-http-aggregator

An `AggregatorController` trait providing a `Route` which authenticates a request and, if successful, makes various HTTP requests to different `AggregatorUpstream`s to assemble an HTTP response. Aggregation behaviour is library-user-specified by providing an `Aggregator` or one of its various subclasses.

_Depends On_: `akka-http-header-authentication`, `akka-http-proxy`\
_Artifact ID_: `akka-http-aggregator`

## License

Copyright 2018, PagerDuty, Inc.

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

test
