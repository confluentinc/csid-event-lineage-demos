# CSID Event Lineage Demo

## Overview

This project demonstrates a credit card transaction flow with event lineage tracing using [OpenTelemetry](https://opentelemetry.io/) (OTel) and Kafka. It simulates a real-time financial system with multiple producers, stream processors, and consumers, instrumented end-to-end to support traceable and observable pipelines. This version runs **without Kafka Connect**. For the variant that includes Kafka Connect, refer to the [`demo-with-connect`](https://github.com/.../tree/demo-with-connect) branch.

👉 [📽️ Demo Slides](https://docs.google.com/presentation/d/1AggKl7_HhRGLwgfzrGNVplR7H4WzeKdgSoKXW_c0a8I/edit#slide=id.g15b4a13f630_0_620)
📖 [📚 Project Documentation](https://bit.ly/3CSnHAH)

---

## Architecture at a Glance

This demo tracks distributed events across services using OpenTelemetry's Java agent and custom extensions.

**Core Components:**

* `OpenTelemetry Java Agent` for auto-instrumentation
* `Custom Header Extensions` for tracking lineage
* `OpenTelemetry Collector` for telemetry routing
* `Kafka Streams` for stateful processing
* `Jaeger`, `Prometheus`, and `Splunk` for observability

**Infrastructure:**

* Single-node Kafka cluster with Schema Registry
* Services written in Java, built with Maven
* Containerised via Docker Compose

---

## Data Flow & Event Lifecycle

The simulation begins with the **data injector**, which pre-loads account and merchant datasets and continuously emits synthetic events for 60 seconds:

### 🔁 Event Producers

| Type          | Frequency   | Payloads                     | Route                                                 |
| ------------- | ----------- | ---------------------------- | ----------------------------------------------------- |
| Account Open  | every 10s   | New account creation         | → `account-event-producer` → Kafka topic: `account`   |
| Account Close | every 30s   | Close existing accounts      | (starts after 30s) → `account-event-producer`         |
| Transactions  | every 100ms | Deposits, Payments, Failures | → `transaction-producer` → Kafka topic: `transaction` |

### 🔀 Kafka Streams Logic

#### Account Stream Branch

* Ingests `account` events into a KTable
* Handles state transitions (open → active, close → inactive)
* Outputs updates to Kafka topic `account-update`

#### Transaction Stream Branch

* Joins transactions with the account KTable
* Validates:

  * Unknown or inactive accounts → reject
  * Active accounts → check balance
* Updates balances and emits results to:

  * `transaction-update`
  * `balance-update`

---

### 🎯 Final Outputs

| Sink Application       | Kafka Topic          | Description                      |
| ---------------------- | -------------------- | -------------------------------- |
| `account-updates-sink` | `account-update`     | Processes account state changes  |
| `transaction-sink`     | `transaction-update` | Handles transaction outcomes     |
| `balance-updates-sink` | `balance-update`     | Updates running account balances |

---

## 🔍 Event Lineage with OpenTelemetry

All services are instrumented with the OpenTelemetry Java agent (`v1.13.0`) and a custom extension for lineage tracking:

* Propagates headers like `account_nr_header` and `system_id`
* Automatically generates and correlates spans
* Enables traceability across producer, stream, and sink layers
* Visualisable via **Jaeger** and searchable in **Splunk**

---

## 🔧 How to Run

### Prerequisites

* Docker
* Java 11+
* Maven

### First-Time Setup

```bash
./run_demo.sh
```

This builds all services, starts the containers, and injects data automatically. Wait \~1–2 minutes for full initialisation.

### Access the UI Components

| Tool                     | URL                                                             |
| ------------------------ | --------------------------------------------------------------- |
| Jaeger (Tracing)         | [http://localhost:16686](http://localhost:16686)                |
| Confluent Control Center | [http://localhost:9021](http://localhost:9021)                  |
| Prometheus (Metrics)     | [http://localhost:9090](http://localhost:9090)                  |
| Splunk (Logs)            | [http://localhost:8000](http://localhost:8000) (admin/abcd1234) |
| OTel Collector Metrics   | [http://localhost:8888](http://localhost:8888)                  |

### Useful Commands

```bash
docker-compose down -v   # Remove all containers and volumes
docker-compose up -d     # Restart without rebuild
```

---

## 📦 Application Components

| Component                | Description                                                         |
| ------------------------ | ------------------------------------------------------------------- |
| `demo-data-injector`     | Emits account and transaction events over HTTP to simulate activity |
| `account-event-producer` | REST service posting account events to Kafka                        |
| `transaction-producer`   | REST service posting transactions to Kafka                          |
| `kstream-app`            | Kafka Streams processor for stateful validation and transformation  |
| `account-updates-sink`   | Kafka consumer writing processed account states                     |
| `balance-updates-sink`   | Kafka consumer maintaining account balances                         |
| `transaction-sink`       | Kafka consumer processing transaction results                       |

---

## 🛠 OpenTelemetry Highlights

* **Auto-instrumentation**: Kafka, HTTP, JVM
* **Custom Propagation**: Application-specific headers for correlation
* **Span Creation & Linking**: Full trace graph from injector to sinks
* **Prometheus Metrics**: JVM, Kafka, and custom app metrics
* **Splunk Logs**: Searchable trace events and errors

---

## Notes

* This version **does not use Kafka Connect**. For that version, see the [`demo-with-connect`](https://github.com/.../tree/demo-with-connect) branch.
* All services are built to simulate a realistic, multi-service topology for testing observability, trace correlation, and lineage propagation.

