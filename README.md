# CSID Data Lineage Demo Project

## Background

This demo is a simple use case showing a simple credit card tracing example for the proposed CSID Data Lineage project.

To learn more about the aims of the project please refer to the documentation at [Data Lineage](https://bit.ly/3CSnHAH)

## Architecture

This demo uses [Open Telemetry](https://opentelemetry.io/) to send telemetry data to downstream targets (Jaeger, Splunk, Elastic, Kafka)

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
* Trace data can be investigated in Elasticsearch through Kibana at http://0.0.0.0:5601
* Trace data can be investigated in Splunk at http://0.0.0.0:8000 with admin/abcd1234 credentials.
To clean up any docker container from the demo run `docker-compose down` from the `/demo` folder.




