# CSID Event Lineage Demo Project

## Background

This demo is a simple use case showing a simple credit card tracing example for the proposed CSID Event Lineage project.
Corresponding demo slides - [Demo slides](https://docs.google.com/presentation/d/1AggKl7_HhRGLwgfzrGNVplR7H4WzeKdgSoKXW_c0a8I/edit#slide=id.g15b4a13f630_0_620)

To learn more about the aims of the project please refer to the documentation at [Event Lineage](https://bit.ly/3CSnHAH)

There are two versions of this demo - with and without Kafka Connect in the flow. This version is without the Kafka Connect. `demo-with-connect` branch has the version with Kafka Connect.

## Architecture Overview

This demo showcases event lineage tracking using OpenTelemetry (OTel) with custom extensions for header propagation and correlation. The system includes:

- **OpenTelemetry Java Agent** (v1.13.0) for automatic instrumentation
- **Custom OTel Extensions** for event lineage header capture and propagation
- **OpenTelemetry Collector** for centralized telemetry processing
- **Jaeger** for distributed tracing visualization
- **Prometheus** for metrics collection
- **Splunk** for log aggregation and analysis

## Running Demo Locally 
Prerequisites:
* Docker
* Java 11 or later
* Maven

First time build:
```
./run_demo.sh
```

Once containers are up after a delay of approximately 1-2 minutes trace/payload information 
will be available in the Jaeger UI at http://0.0.0.0:16686

In addition:
* Confluent Control Centre is available at http://0.0.0.0:9021
* Metrics are made available in Prometheus at http://0.0.0.0:9090
* Trace data can be investigated in Splunk at http://0.0.0.0:8000 with admin/abcd1234 credentials.
* OpenTelemetry Collector metrics are available at http://0.0.0.0:8888
* To clean up any docker container from the demo run `docker-compose down -v` from the `/demo` folder.
* To restart the demo without rebuilding containers - `docker-compose down -v` and then `docker-compose up -d` from the `/demo` folder.

## Demo application composition

* `demo-data-injector` - A simple mock data generator - generates Account open/close and Transaction send/withdraw events while keeping data correlated. Runs for 60 seconds by default.

* `account-event-producer` - REST web service accepting Account open/close events from data injector and publishing to Kafka topic. Runs on port 7070. `account-producer` service in trace data.

* `transaction-producer` - REST web service accepting Transaction send/withdraw events from data injector and publishing to Kafka topic. Runs on port 7071. `transaction-producer` service in trace data.

* `kstream-app` - Kafka Streams application consuming account and transaction events and then processing them using state-full operations. Account state is maintained in a KTable, Balance is maintained as an Aggregate operation of all transactions grouped by Account number. `account-processor` service in trace data.

* `account-updates-sink` - Kafka consumer - sink app for account updates. `account-update-consumer` service in trace data.

* `balance-updates-sink` - Kafka consumer - sink app for balance updates. `balance-update-consumer` service in trace data.

* `transaction-sink` - Kafka consumer - sink app for transaction updates. `transaction-status-consumer` service in trace data.

## OpenTelemetry Features

The demo includes several OpenTelemetry features:

- **Automatic Instrumentation**: All Kafka producers, consumers, and HTTP endpoints are automatically instrumented
- **Custom Header Propagation**: Headers like `account_nr_header` and `system_id` are captured and propagated across services
- **Distributed Tracing**: Complete request flows are traced from data injection through processing to final sinks
- **Metrics Collection**: Application and Kafka metrics are collected and exposed via Prometheus
- **Custom Extensions**: The demo uses custom OpenTelemetry extensions for enhanced event lineage tracking

## Infrastructure Components

- **Kafka Broker**: Single-node Kafka cluster with KRaft mode
- **Schema Registry**: Confluent Schema Registry for schema management
- **OpenTelemetry Collector**: Centralized telemetry processing and routing
- **Jaeger**: Distributed tracing backend and UI
- **Prometheus**: Metrics collection and storage
- **Splunk**: Log aggregation and analysis platform
- **Confluent Control Center**: Kafka cluster management and monitoring

