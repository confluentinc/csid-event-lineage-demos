#!/bin/bash

HEADER="Content-Type: application/json"
DATA=$( cat << EOF
{
  "name": "dc1-to-dc2",
  "config": {
    "connector.class": "io.confluent.connect.replicator.ReplicatorSourceConnector",
    "topic.whitelist": "account-input,transaction-input",
    "key.converter": "io.confluent.connect.replicator.util.ByteArrayConverter",
    "value.converter": "io.confluent.connect.replicator.util.ByteArrayConverter",
    "header.converter": "io.confluent.connect.replicator.util.ByteArrayConverter",
    "src.kafka.bootstrap.servers": "kafka:29092",
    "src.consumer.group.id": "replicator-dc2-to-dc1-topic1",
    "src.kafka.timestamps.topic.replication.factor": 1,
    "dest.kafka.bootstrap.servers": "broker-dc1:29091",
    "confluent.topic.replication.factor": 1,
    "provenance.header.enable": "true",
    "tasks.max": "2",
    "transforms": "insertAppIdHeader",
    "transforms.insertAppIdHeader.type": "com.testing.kafka.connect.smt.InsertHeaderBytes",
    "transforms.insertAppIdHeader.header": "system_id",
    "transforms.insertAppIdHeader.value.literal": "test-system"
  }
}
EOF
)

curl -X POST -H "${HEADER}" --data "${DATA}" http://localhost:8381/connectors
