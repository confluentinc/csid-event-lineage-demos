#!/bin/bash
suppress_receive=true

sleep 5 #give some time for kafka cluster to finish init on startup.
#Start kstream services first

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.7.2.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-processor \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=account-processor \
           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &
sleep 10

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.7.2.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-service \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dotel.javaagent.debug=true \
           -Dapp=account-service \
           -jar account-event-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.7.2.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-service \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dotel.javaagent.debug=true \
           -Dapp=transaction-service \
           -jar transaction-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

sleep 10

# output consumers
nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.7.2.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-sink \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -jar transaction-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.7.2.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=account-updates-sink \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -jar account-updates-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-1.7.2.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=balance-updates-sink \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -jar balance-updates-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

#Give time for consumers to subscribe / initialize
sleep 10
#Start data injector and run for 3 minutes.
#Transactions producer
nohup java -jar demo-data-injector-0.0.1-SNAPSHOT-jar-with-dependencies.jar 180 &
#keep process alive to keep container up.
/bin/bash -c "trap : TERM INT; sleep infinity & wait"
