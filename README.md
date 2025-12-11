# Delivery Hours Service

Your task is to implement the Delivery Hours Service!

This README.md describes the development setup for the Delivery Hours Service as well as what we expect from you.
[SYSTEM_SPECIFICATION.md](./SYSTEM_SPECIFICATION.md) contains detailed specifications of our Delivery Hours Service and its dependencies.

To make this assignment as realistic as possible, we have made some technology choices which align with what we commonly use in our Kotlin backend projects at Wolt.
You'll be using Ktor as a web framework, JUnit for testing, KtLint for formatting and linting, and Gradle for managing dependencies.
There's some boilerplate code to get you up and running.
Additionally, there's a Docker Compose setup and a Makefile included for convenient development.

It's perfectly ok if you don't have previous experience with all the used technologies.
The provided boilerplate code will get you up and running quickly anyway.
For example, previous experience with Ktor is not expected.
If some of technologies used in this project are new to you, please mention them in the _Notes from the applicant_ section in this README.

## Your task
**Implement the _GET /delivery-hours_ endpoint and tests for it. See the specification in [SYSTEM_SPECIFICATION.md](./SYSTEM_SPECIFICATION.md)**

There's already a placeholder for the endpoint in the provided code. Additionally, there's an example test case which should pass once you have implemented the endpoint.

### How to do it in practice
1. Clone this repository
2. Checkout a new branch `git checkout -b my-implementation`
3. Implement your solution and push the changes to your branch
4. Once you're done
    1. Open a PR (pull request). Feel free to add comments in the PR.
    2. Inform the recruiter that you've finished the assignment

### Expectations
We expect you to:
* Follow the specification described in the [SYSTEM_SPECIFICATION.md](./SYSTEM_SPECIFICATION.md).
* Adjust the existing code as you see fit. Also, feel free to introduce new dependencies if needed.
* Have a reasonable architecture for the Delivery Hours Service. Feel free to restructure and refactor the existing code as much as you want. Feel free to introduce new modules and packages.
* Implement tests for your solution.
* Consider that this could be a real world project so the code quality should be on the level that you'd be happy to contribute in our real projects.
* Use your judgement in case you discover an edge case which is not documented in the [SYSTEM_SPECIFICATION.md](./SYSTEM_SPECIFICATION.md). In such cases, please document your choices / assumptions here in the README or in the pull request.

We **do not** expect you to:
* Adjust the `ci.yml`, `Dockerfile`, `compose.yml`, `Makefile`, or `src/test/resources/external-services-mock/stub.json`.
* Adjust the service's endpoint path, query parameters or output schema.
* Introduce authentication.
* Introduce monitoring.
* Introduce any persistence (database).
* Deploy your solution.

## Development
Prerequisites:
* Docker Compose V2 (aka `docker compose` without the `-` in between)
* Make
* Java 21

### Essentials
Get everything up and running:
```shell
make start
```
or just:
```shell
./gradlew run
```

This starts our Delivery Hours Service and a [WireMock](https://wiremock.org/) container which acts as a mock for Venue Service and Courier Service.

You can access Delivery Hours Service from localhost via port 8000 and the external services via port 8080.

Bring everything down:
```shell
make stop
```

For convenience, there's also `make restart` which runs `stop` and `start`.

### Testing
You can run the tests with Gradle using:
```shell
make test
```

It doesn't matter whether you have the containers already running (i.e. if you have `make start` running in another shell window) or not, `make test` works either way.

### Linting
We are using `ktlint` for linting.

Run linter with:
```shell
make lint
```

To format everything run:
```shell
make format
```

## URLs
The urls that are accessible from localhost:
* Delivery Hours Service (our own service): http://localhost:8000 (e.g. http://localhost:8000/delivery-hours?city_slug=helsinki&venue_id=123)
* Courier Service (mocked): http://localhost:8080/courier-service (e.g. http://localhost:8080/courier-service/delivery-hours?city=helsinki)
    * Note that the mock is only configured for _helsinki_, the response is 404 for other cities
* Venue Service (mocked): http://localhost:8080/venue-service (e.g. http://localhost:8080/venue-service/venues/123/opening-hours)
    * Note that the mock is only configured for _123_, the response is 404 for other venue ids

## Continuous integration aka CI

There's a GitHub workflow (ci.yml) which runs linter for the whole codebase and the test suite on each push.
Make sure your implementation passes the CI before submitting your solution.

## Notes from the applicant

First of all, I want to say thank you for offering to do this test assignment for me. It was an interesting challenge. 
A few words about my solution.

### Edge case: crossing midnight with a 06:00 cut

**Day boundary rule.** For display purposes, a “day” ends at **06:00**, not at 00:00.

1. **Evening→early-morning stays in the start day.**  
   Example: `21:00–05:00` is displayed in the same day that has `21:00`.
2. **Any interval crossing 06:00 is split exactly at 06:00.**  
   Example: `21:00–08:00` → `21:00–06:00` (start day) and `06:00–08:00` (next day).

**Minimum slot length (paired across 06:00).** After splitting at 06:00, a per-day segment shorter than **30 minutes** may appear. It is **kept** if it is the continuation of the **same logical overnight interval** and the **combined duration** of the two paired parts (before 06:00 + after 06:00) is **≥ 30 minutes**. Otherwise, sub-30-minute segments are **discarded** and the day is **`Closed (less than 30 mins)`** if nothing valid remains.

#### Crossing 06:00 cut

| Raw hours     | Display with 06:00 cut                                   | Notes                                                                 |
|---------------|---|---|
| `21:00–05:00` | `21:00–05:00` (same day)                                 | Ends before 06:00 → no split                                          |
| `21:00–08:00` | `21:00–06:00` (day D); `06:00–08:00` (day D+1)           | Split exactly at 06:00; both parts kept                               |
| `23:30–06:10` | `23:30–06:00` (day D); `06:00–06:10` (day D+1)           | Post-cut part = 10 min < 30, **but** paired total ≥ 30 → **kept**     |
| `05:50–06:10` | ~~`05:50–06:00`~~; ~~`06:00–06:10`~~                     | Total = 20 min < 30 and no longer span → **discarded**                |
| `05:45–06:15` | `05:45–06:00` (day D); `06:00–06:15` (day D+1)           | Paired total = 30 min → **both kept**                                 |


### Future Improvements

While the current implementation fulfills the requirements and is robust enough for the specified task, here is how I would evolve the service for a high-load production environment:

* **Caching (Redis):**
  The delivery and opening hours are relatively static data. Fetching them from upstream services on every request is inefficient. I would introduce a caching layer (e.g., **Redis**) with a reasonable TTL (Time-To-Live). This would significantly reduce latency and the load on Venue/Courier services.

* **Communication Protocol (gRPC):**
  Currently, the service communicates via HTTP/JSON. For internal microservice communication, I would migrate to **gRPC (Protobuf)**. It offers strict contract definitions, better performance (binary format, smaller payloads), and lower latency compared to REST.

* **API Documentation:**
  Integrate **OpenAPI/Swagger** to automatically generate documentation for the `GET /delivery-hours` endpoint, making it easier for consumers to integrate with the service.

### Final note

Everything else works as intended: all commands, validations, and edge-case handling are implemented as specified. Thank you for the opportunity to complete this assignment and for the chance to be considered.