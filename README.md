# CSID Event Lineage Demo Project

## Overview

This project demonstrates a simplified credit card transaction lineage flow as part of the proposed **CSID Event Lineage** initiative.

- [Presentation Slides](https://docs.google.com/presentation/d/1AggKl7_HhRGLwgfzrGNVplR7H4WzeKdgSoKXW_c0a8I/edit#slide=id.g15b4a13f630_0_620)  
- [Project Documentation](https://bit.ly/3CSnHAH)

There are two versions of this demo:
- **This branch**: Kafka Streams-based flow (no Kafka Connect)
- **[`demo-with-connect`](https://github.com/confluentinc/csid-event-lineage-demos/tree/demo-with-connect)**: Includes Kafka Connect in the lineage pipeline

---

## Running the Demo Locally

### Prerequisites

- Docker
- Java 11 or later
- Maven

### First-time Setup

From the project root, run:

```bash
./run_demo.sh
````

This script builds and starts all required services. Initial container startup may take 1 to 2 minutes.

---

## Observability

Once up and running, you can access the following services:

| Component      | URL                                              | Description                              |
| -------------- | ------------------------------------------------ | ---------------------------------------- |
| Jaeger UI      | [http://localhost:16686](http://localhost:16686) | View end-to-end traces                   |
| Control Center | [http://localhost:9021](http://localhost:9021)   | Confluent monitoring dashboard           |
| Prometheus     | [http://localhost:9090](http://localhost:9090)   | Metrics and system observability         |
| Splunk         | [http://localhost:8000](http://localhost:8000)   | Trace logs (Login: `admin` / `abcd1234`) |

---

## Demo Lifecycle Commands

| Action                     | Command                                                                |
| -------------------------- | ---------------------------------------------------------------------- |
| Stop and remove containers | `docker-compose down -v` (run from `/demo` directory)                  |
| Restart without rebuilding | `docker-compose down -v && docker-compose up -d` (from `/demo` folder) |

---

## Demo Components

Schemas for the JSON payloads used by the services below are in `common/src/main/java/io/confluent/csid/data/governance/lineage/opentel/transactiondemo/common/domain`

Constants (including topic names) are in `common/src/main/java/io/confluent/csid/data/governance/lineage/opentel/transactiondemo/common/Constants.java`

| Component                | Description                                                                                                                                                                                                                       | Trace Name                    |
| ------------------------ |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------| ----------------------------- |
| `demo-data-injector`     | Generates mock events: Account open/close and Transaction send/withdraw <br/>                                                                                                                                                     | N/A                           |
| `account-event-producer` | REST service publishing account events to Kafka > POST on http://localhost:7070/produce-account-event                                                                                                                             | `account-producer`            |
| `transaction-producer`   | REST service publishing transaction events to Kafka > POST on http://localhost:7071/produce-transaction-event                                                                                                                     | `transaction-producer`        |
| `kstream-app`            | Kafka Streams application with stateful processing. Maintains account state and aggregates. <br/> Publishes account updates to a topic, verifies transactions integrity (do accounts exist? do accounts are sufficiently funded?) | `account-processor`           |
| `account-updates-sink`   | Kafka consumer writing account updates to the console output                                                                                                                                                                      | `account-update-consumer`     |
| `balance-updates-sink`   | Kafka consumer writing balance updates to the console output                                                                                                                                                                      | `balance-update-consumer`     |
| `transaction-sink`       | Kafka consumer writing transaction events to the console output                                                                                                                                                                   | `transaction-update-consumer` |

---

For any issues or contributions, please open a GitHub issue or submit a pull request.
