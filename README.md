# CSID Event Lineage Demo Project

## Background

This demo is a simple use case showing a simple credit card tracing example for the proposed CSID Event Lineage project.
Corresponding demo slides - [Demo slides](https://docs.google.com/presentation/d/1AggKl7_HhRGLwgfzrGNVplR7H4WzeKdgSoKXW_c0a8I/edit#slide=id.g15b4a13f630_0_620)

To learn more about the aims of the project please refer to the documentation at [Event Lineage](https://bit.ly/3CSnHAH)

There are two versions of this demo - with and without Kafka Connect in the flow. This version is without the Kafka Connect. `demo-with-connect` branch has the version with Kafka Connect.

## Running Demo Locally 
Prequisites:
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
* To clean up any docker container from the demo run `docker-compose down -v` from the `/demo` folder.
* To restart the demo without rebuilding containers - `docker-compose down -v` and then `docker-compose up -d` from the `/demo` folder.

## Demo application composition
* `demo-data-injector` - A simple mock data generator - generates Account open/close and Transaction send/withdraw events while keeping data correlated.

* `account-event-producer` - Rest web service accepting Account open/close events from data injector and publishing to Kafka topic. `account-producer` service in trace data.

* `transaction-producer` - Rest web service accepting Transaction send/withdraw events from data injector and publishing to Kafka topic. `transaction-producer` service in trace data.

* `kstream-app` - Kafka Streams application consuming account and transaction events and then processing them using state-full operations. Account state is maintained in a KTable, Balance is maintained as an Aggregate opperation of all transactions grouped by Account number. `account-processor` service in trace data.

* `account-updates-sink` - Kafka consumer - sink app for account updates. `account-update-consumer` service in trace data.

* `balance-updates-sink` - Kafka consumer - sink app for balance updates. `balance-update-consumer` service in trace data.

* `transaction-sink` - Kafka consumer - sink app for transaction updates. `transaction-update-consumer` service in trace data.

