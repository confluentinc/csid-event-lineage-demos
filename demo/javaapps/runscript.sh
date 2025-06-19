#!/bin/bash
set -e

suppress_receive=true
header_config=" -Dotel.javaagent.debug=true -Devent.lineage.header-capture-whitelist=account_nr_header,system_id -Devent.lineage.header-propagation-whitelist=account_nr_header,system_id -Devent.lineage.header-charset=UTF-8 "

sleep 5 # Give Kafka cluster time to initialise

# Start kstream services first
nohup java -XX:-UseContainerSupport \
           -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-processor \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=account-processor \
           ${header_config} \
           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

sleep 10

# Start account producer
nohup java -XX:-UseContainerSupport \
           -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-producer \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=account-producer \
           ${header_config} \
           -jar account-event-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

# Start transaction producer
nohup java -XX:-UseContainerSupport \
           -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-producer \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=transaction-producer \
           ${header_config} \
           -jar transaction-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

sleep 10

# Start consumers
nohup java -XX:-UseContainerSupport \
           -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-status-consumer \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           ${header_config} \
           -jar transaction-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -XX:-UseContainerSupport \
           -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-update-consumer \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           ${header_config} \
           -jar account-updates-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -XX:-UseContainerSupport \
           -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=balance-update-consumer \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           ${header_config} \
           -jar balance-updates-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

# Allow consumers to subscribe
sleep 10

# Start data injector
nohup java -XX:-UseContainerSupport \
           -jar demo-data-injector-0.0.1-SNAPSHOT-jar-with-dependencies.jar 60 &

# Keep container running
/bin/bash -c "trap : TERM INT; sleep infinity & wait"
