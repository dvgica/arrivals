# Arrivals [![CircleCI](https://circleci.com/gh/PagerDuty/arrivals.svg?style=svg)](https://circleci.com/gh/PagerDuty/arrivals)

This open-source project provides building blocks for constructing an API Gateway using Akka HTTP. [PagerDuty](https://www.pagerduty.com) has [used it](https://www.youtube.com/watch?v=DRxLFWmvJ8A) in production to build a few [BFFs](https://samnewman.io/patterns/architectural/bff/). Currently, the project is focused on the following areas:

 - Proxying HTTP requests to upstream services
 - Authenticating those requests
 - Adding a header to prove authentication to upstream services
 - Filtering and transforming requests and responses
 - Turning a single request into multiple upstream requests, and aggregating the responses into a single response
  
As much as possible, Arrivals follows idioms found in Akka HTTP. This means that it exposes `Route`s and `Directive`s to the library user which can be combined with other Akka HTTP-based code.  

## Usage

Arrivals functionality is provided via Akka HTTP `Route`s available in `AuthProxyRoutes`, `ProxyRoutes`, and `AggregatorRoutes`. Other lower-level `Directive`s are also available.

A basic example of using Arrivals is available in [arrivals-example](https://github.com/PagerDuty/arrivals/blob/master/arrivals-example/src/main/scala/com/pagerduty/arrivals/example/ExampleApp.scala). The example can be run by cloning this repository and running `sbt arrivalsExample/run`. Try the following URLs:

- http://localhost:8080/cats
- http://localhost:8080/dogs
- http://localhost:8080/dogs?username=rex
- http://localhost:8080/all?username=rex

More docs to come.

## Artifacts

All artifacts are published with the `com.pagerduty` `groupId` and are available at the PagerDuty Bintray OSS repository.

Add the PD Bintray to your resolvers with the following:

```
resolvers += "bintray-pagerduty-oss-maven" at "https://dl.bintray.com/pagerduty/oss-maven"
```

### arrivals

This is the implementation artifact on which applications should depend.

_Depends On_: `arrivals-api`, `akka-http`, and PD's [`metrics-api`](https://github.com/PagerDuty/scala-metrics)\
_Artifact ID_: `arrivals`

### arrivals-api

Authors of custom implementations (e.g. `Filter`s, `Upstream`s, `Aggregator`s, and `RequestResponder`s) should depend on this artifact, which will hopefully change less frequently.

_Depends On_: `akka-http`, and PD's [`scala-akka-support`](https://github.com/PagerDuty/scala-akka-support)\
_Artifact ID_: `arrivals-api`

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

## TODO

- Docs and examples
- Metadata logging is inconsistently used because it's a PITA - would be nice to do something less ugly and not include `akka-http-support` in `arrivals-api`
- De-couple authentication and authorization
- Allow for customizeable handling of multiple credentials
