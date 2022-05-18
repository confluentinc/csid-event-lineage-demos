#!/bin/bash
suppress_receive=true
header_config=" -Devent.lineage.header-capture-whitelist=account_nr_header,system_id -Devent.lineage.header-propagation-whitelist=account_nr_header,system_id -Devent.lineage.header-charset=UTF-8 "
sleep 5 #give some time for kafka cluster to finish init on startup.
#Start kstream services first

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-processor \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=account-processor \
           ${header_config} \
           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &
sleep 10

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-service \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dotel.javaagent.debug=true \
           -Dapp=account-service \
           ${header_config} \
           -jar account-event-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-service \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dotel.javaagent.debug=true \
           -Dapp=transaction-service \
           ${header_config} \
           -jar transaction-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

sleep 10

# output consumers
nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-sink \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           ${header_config} \
           -jar transaction-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-updates-sink \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           ${header_config} \
           -jar account-updates-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.13.0.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=balance-updates-sink \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           ${header_config} \
           -jar balance-updates-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

#Give time for consumers to subscribe / initialize
sleep 10
#Start data injector and run for 3 minutes.
#Transactions producer
nohup java -jar demo-data-injector-0.0.1-SNAPSHOT-jar-with-dependencies.jar 60 &
#keep process alive to keep container up.
/bin/bash -c "trap : TERM INT; sleep infinity & wait"
