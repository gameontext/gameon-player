# Game On! Player Service 

[![Codacy Badge](https://api.codacy.com/project/badge/grade/1fc932713e474ba0bb9593a9cdcb8e35)](https://www.codacy.com/app/gameontext/gameon-player)

See the [application architecture description](https://gameontext.gitbooks.io/gameon-gitbook/content/microservices/) in the Game On! Docs for more information on how to use this service.

## Building

To build this project: 

    ./gradlew build
    docker build -t gameontext/gameon-player player-wlpcfg

## [MicroProfile](https://microprofile.io/)
MicroProfile is an open platform that optimizes the Enterprise Java for microservices architecture. In this application, we are using [**MicroProfile 1.3**](https://github.com/eclipse/microprofile-bom).

### Features
1. [MicroProfile Metrics](https://github.com/eclipse/microprofile-metrics) - This feature allows us to expose telemetry data. Using this, developers can monitor their services with the help of metrics.

    The application uses the `Timed`, `Counted` and `Metered` metrics. To access these metrics, go to https://localhost:9448/metrics.
    The Metrics feature is configured with SSL and can only be accessed through https. You will need to login using the username and password configured in the server.xml. The default values are `admin` and `admin`.

2. [MicroProfile Health Check](https://github.com/eclipse/microprofile-health) - This feature helps us to determine the status of the service as well as its availability. This can be checked by accessing the `/health` endpoint.

3. [MicroProfile Open Tracing](https://github.com/eclipse/microprofile-opentracing) -  This feature enables distributed tracing. It analyzes the transaction flow and allows for custom tracing of JAX-RS and non-JAX-RS methods.

    We are using [Zipkin](https://zipkin.io/) as the distributed tracing system. It is setup to run through the root project's Docker Compose, and can be accessed at https://localhost:9411.

4. [MicroProfile Fault Tolerance](https://github.com/eclipse/microprofile-fault-tolerance) - These features help reduce the impact of failure and ensure continued operation of services. This project uses Fallback, Retry, and Timeout.

5. [MicroProfile OpenAPI](https://github.com/eclipse/microprofile-open-api) - This feature, built on Swagger, provides a set of Java interfaces and programming models that allow Java developers to natively produce OpenAPI v3 documents from their JAX-RS applications. 

    This project uses Swagger and JAX-RS annotations to document the application endpoints. To view the API docs, go to https://localhost:9448/openapi/ui/.

## Contributing

Want to help! Pile On! 

[Contributing to Game On!](CONTRIBUTING.md)
