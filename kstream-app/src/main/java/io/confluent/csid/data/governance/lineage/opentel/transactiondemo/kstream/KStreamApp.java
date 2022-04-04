/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.kstream;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.Topics;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.*;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountEvent.AccountEventType;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent.TransactionType;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus.Status;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.*;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;

@SuppressWarnings({ "WeakerAccess", "unused" })
@Slf4j
public class KStreamApp {

    final CountDownLatch latch = new CountDownLatch(1);
    final String appName;
    final String bootstrapServer;
    KafkaStreams streams;
    public void awaitFinish() throws InterruptedException {
        latch.await();
    }

    public KStreamApp(String appName, String bootstrapServer) {
        this.appName = appName;
        this.bootstrapServer = bootstrapServer;
    }

    static TextMapGetter<Set<Entry<String, byte[]>>> getter = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Set<Entry<String, byte[]>> headers) {
            return headers.stream()
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
        }

        @Override
        public String get(Set<Entry<String, byte[]>> headers, String key) {
            if (headers == null) {
                return null;
            }
            return headers.stream()
                    .filter(e -> e.getKey().equals(key))
                    .findFirst()
                    .map(Entry::getValue)
                    .map(v -> new String(v, StandardCharsets.UTF_8))
                    .orElse(null);

        }
    };

    private static Properties getProperties(Class<?> valueSerde, String applicationId, String bootstrapServer) {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                (bootstrapServer == null) ? BOOTSTRAP_KAFKA_SERVER : bootstrapServer);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde);
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000L);

        // setting offset reset to earliest so that we can re-run the demo code with the
        // same pre-loaded data
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    public void runStreams() {
        final StreamsBuilder builder = new StreamsBuilder();
        Properties props;
        props = getProperties(JsonAccountEventSerde.class, appName, bootstrapServer);
        recreateTopics(props);

        JsonAccountEventSerde jsonAccountEventSerde = new JsonAccountEventSerde();
        JsonAccountSerde jsonAccountSerde = new JsonAccountSerde();
        JsonAccountBalanceSerde jsonAccountBalanceSerde = new JsonAccountBalanceSerde();
        KTable<String, Account> accountKTable = builder.stream(
          Topics.ACCOUNT_IN,
          Consumed.with(Serdes.String(), jsonAccountEventSerde)
        ).mapValues(accountEvent -> {
            if (accountEvent.getAccountEventType() == AccountEventType.OPEN) {
                return new Account(accountEvent.getAccountNr(), accountEvent.getAccountHolder(), true);
            } else {
                return new Account(accountEvent.getAccountNr(), null, false);
            }
        })
        .toTable(
            Named.as(Topics.ACCOUNT_KTABLE),
            Materialized.with(Serdes.String(), jsonAccountSerde)
        );

        JsonTransactionEventSerde jsonTransactionEventSerde = new JsonTransactionEventSerde();
        builder.stream(
            Topics.TRANSACTION_IN,
            Consumed.with(Serdes.String(), jsonTransactionEventSerde)
        )
        .join(accountKTable, Pair::of).split().branch(
            (k, v) -> v.getRight() == null || !v.getRight().isActive(),
            Branched.withConsumer(x -> x.mapValues(v -> {
                TransactionEvent event = v.getLeft();
                if (v.getRight() == null) {
                    event.setStatus(new TransactionStatus(Status.REJECTED, "Unknown Account"));
                } else {
                    event.setStatus(new TransactionStatus(Status.REJECTED, "Inactive Account"));
                }
                return event;
            }).to(Topics.TRANSACTION_OUTPUT_TOPIC, Produced.with(Serdes.String(), jsonTransactionEventSerde)))
        )
        .defaultBranch(Branched.withConsumer(x -> {
            KStream<String, AccountBalance> balanceStream =
                x.groupByKey().aggregate(AccountBalance::new, (key, value, aggregate) -> {
                    String accountNr = value.getLeft().getAccountNr();
                    AccountBalance accountBalance =
                        aggregate.getAccountNr() == null ? new AccountBalance(accountNr,
                    BigDecimal.valueOf(0), null) : aggregate;
                    BigDecimal amount = value.getLeft().getAmount();
                    TransactionType transactionType = value.getLeft().getTransactionType();
                    if (isPaymentOverAvailableBalance(value.getLeft(), accountBalance)) {
                        value.getLeft().setStatus(new TransactionStatus(Status.REJECTED, "Insufficient funds"));
                    } else {
                        accountBalance.setBalance((
                            transactionType.equals(TransactionType.DEPOSIT) ?
                            accountBalance.getBalance().add(amount) :
                            accountBalance.getBalance().subtract(amount)
                        ));
                        value.getLeft().setStatus(new TransactionStatus(Status.PROCESSED, ""));
                    }
                    accountBalance.setLastTransaction(value.getLeft());
                    return accountBalance;
                },
                Named.as("account-balances"),
                Materialized.with(Serdes.String(), jsonAccountBalanceSerde)
            )
            .toStream();
            balanceStream.mapValues(AccountBalance::getLastTransaction)
                .to(Topics.TRANSACTION_OUTPUT_TOPIC, Produced.with(Serdes.String(), jsonTransactionEventSerde));
            balanceStream.to(Topics.BALANCE_UPDATES_TOPIC);
        }));

        JsonSpansSerde jsonSpansSerde = new JsonSpansSerde();
        JsonSpanSerde jsonSpanSerde = new JsonSpanSerde();
        builder.stream(Topics.SPAN_TOPIC, Consumed.with(Serdes.String(), jsonSpanSerde))
            .groupByKey()
            .aggregate(ArrayList<Span>::new, (traceId, span, aggregate) -> {
                    aggregate.add(span);
                    return aggregate;
                },
                Named.as("grouped-spans"),
                Materialized.with(Serdes.String(), jsonSpansSerde)
            )
            .toStream()
            .to(Topics.GROUPED_SPAN_TOPIC);

        streams = new KafkaStreams(builder.build(), props);
        streams.setUncaughtExceptionHandler(throwable -> {
            throwable.printStackTrace();
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });
        streams.cleanUp();
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().
                addShutdownHook(new Thread("streams-" + appName + "-shutdown-hook") {
                  @Override
                  public void run() {
                    streams.close();
                    latch.countDown();
                  }
                });

        try {
            streams.start();
        } catch (final Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        if (streams != null) {
            streams.close();
            streams = null;
            latch.countDown();
        }
    }

    public static void main(final String[] args) {
        KStreamApp app = new KStreamApp(System.getProperty("app"), System.getProperty("bootstrapServer"));
        try {
            app.runStreams();
            app.awaitFinish();
        } catch (Exception e) {
            System.exit(1);
        }
        System.exit(0);
    }

    private boolean isPaymentOverAvailableBalance(TransactionEvent left,
            AccountBalance right) {
        return (left.getTransactionType() == TransactionType.PAYMENT
                && left.getAmount().compareTo(right.getBalance()) > 0);
    }

    private void deleteTopic(String topicName, AdminClient adminClient) {
        try {
            adminClient.deleteTopics(
                    Collections.singleton(topicName)).all().get();
            Thread.sleep(500);
        } catch (Exception e) {
            log.warn("Error deleting topic ", e);
        }
    }

    private void recreateTopics(Properties props) {
        List<String> topics = List.of(
            Topics.ACCOUNT_IN,
            Topics.ACCOUNT_KTABLE,
            Topics.ACCOUNT_OUTPUT_TOPIC,
            Topics.TRANSACTION_IN,
            Topics.TRANSACTION_PROCESSING_TOPIC,
            Topics.TRANSACTION_OUTPUT_TOPIC,
            Topics.BALANCE_UPDATES_TOPIC,
            Topics.SPAN_TOPIC
        );

        List<String> compactedTopics = List.of(
            Topics.GROUPED_SPAN_TOPIC
        );

        AdminClient kafkaAdminClient = AdminClient.create(props);

        var topicResult = kafkaAdminClient.createTopics(
            topics.stream()
                .map(topic -> new NewTopic(topic, 1, (short) 1))
                .collect(Collectors.toList())
        );

        var compactedTopicResult = kafkaAdminClient.createTopics(
            compactedTopics.stream()
                .map(topic -> new NewTopic(topic, 1, (short) 1)
                    .configs(Map.of(
                        "cleanup.policy", "compact"
                    )))
                .collect(Collectors.toList())
        );

        try {
            topicResult.all().get();
            compactedTopicResult.all().get();
        } catch (Exception e) {
            log.warn("Error creating topic ", e);
        }
        int count = 0;

        do {
            try {
                Thread.sleep(100);
                count = (int) kafkaAdminClient.listTopics().names().get().stream()
                        .filter(topic -> topics.contains(topic) || compactedTopics.contains(topic))
                        .count();
            } catch (Exception e) {
                log.warn("Error creating topic ", e);
            }
        } while (count != (topics.size() + compactedTopics.size()));
    }
}
