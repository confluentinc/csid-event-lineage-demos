#!/bin/bash
suppress_receive=true

sleep 5 #give some time for kafka cluster to finish init on startup.
#Start kstream services first

#nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-all.jar \
#           -Dotel.resource.attributes=service.name=card-enrichment \
#           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
#           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
#           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
#           -Dapp=card-enrichment \
#           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &
#sleep 10
nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-all.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/instrumentation-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=card-enrichment-ktable \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=card-enrichment-ktable \
           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &
sleep 10
nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-all.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/instrumentation-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=balance-verification \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=balance-verification \
           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-all.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/instrumentation-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=entity-enrichment \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=entity-enrichment \
           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-all.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/instrumentation-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=fraud-detection \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=fraud-detection \
           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-all.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/instrumentation-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-processor \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -Dapp=transaction-processor \
           -jar kstream-app-0.0.1-SNAPSHOT-jar-with-dependencies.jar &
sleep 10

# output consumer
nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-all.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/instrumentation-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-sink \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -jar transaction-sink-0.0.1-SNAPSHOT-jar-with-dependencies.jar &

#Give time for consumers to subscribe / initialize
sleep 10
#Start producers
#Transactions producer
nohup java -javaagent:/usr/src/javaapps/opentelemetry-javaagent-all.jar \
           -Dotel.javaagent.extensions=/usr/src/javaapps/instrumentation-0.0.1-SNAPSHOT-all.jar \
           -Dotel.resource.attributes=service.name=transaction-producer \
           -Dotel.instrumentation.kafka.experimental-span-attributes=true \
           -Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=${suppress_receive} \
           -Dotel.exporter.otlp.endpoint=http://otel-collector:4317/ \
           -jar transaction-producer-0.0.1-SNAPSHOT-jar-with-dependencies.jar &
#keep process alive to keep container up.
/bin/bash -c "trap : TERM INT; sleep infinity & wait"
