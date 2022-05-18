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

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER_2;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.Topics;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Account;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountBalance;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountEvent.AccountEventType;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent.TransactionType;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus.Status;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonAccountBalanceSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonAccountEventSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonAccountSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonTransactionEventSerde;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Aggregator;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Initializer;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueMapper;

@SuppressWarnings({"WeakerAccess", "unused"})
@Slf4j
public class KStreamApp {

  static TextMapGetter<Set<Entry<String, byte[]>>> getter =
      new TextMapGetter<>() {
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

  private static Properties getProperties(Class<?> valueSerde, String applicationId) {
    final Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER_2);
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerde);
    props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000L);

    // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return props;
  }

  public static void main(final String[] args) {
    String app = System.getProperty("app");

    final StreamsBuilder builder = new StreamsBuilder();
    Properties props;
    props = getProperties(JsonAccountEventSerde.class, app);
    recreateTopicsFor2Clusters(props);
    JsonAccountEventSerde jsonAccountEventSerde = new JsonAccountEventSerde();
    JsonAccountSerde jsonAccountSerde = new JsonAccountSerde();
    JsonAccountBalanceSerde jsonAccountBalanceSerde = new JsonAccountBalanceSerde();
    KTable<String, Account> accountKTable = builder.stream(Topics.ACCOUNT_IN,
            Consumed.with(Serdes.String(), jsonAccountEventSerde)).mapValues(
            (ValueMapper<AccountEvent, Account>) accountEvent -> {
              if (accountEvent.getAccountEventType() == AccountEventType.OPEN) {
                return new Account(accountEvent.getAccountNr(), accountEvent.getAccountHolder(),
                    true);
              } else {
                return new Account(accountEvent.getAccountNr(), null, false);
              }
            })
        .toTable(Named.as(Topics.ACCOUNT_KTABLE),
            Materialized.with(Serdes.String(), jsonAccountSerde));
    JsonTransactionEventSerde jsonTransactionEventSerde = new JsonTransactionEventSerde();

    KStream<String, Pair<TransactionEvent, Account>>[] branches = builder.stream(
        Topics.TRANSACTION_IN,
        Consumed.with(Serdes.String(), jsonTransactionEventSerde)).join(accountKTable,
        Pair::of).branch(
        (k, v) -> v.getRight() == null || !v.getRight().isActive(),
        (k, v) -> true);

    branches[0].mapValues(v -> {
      TransactionEvent event =
          v.getLeft();
      if (v.getRight() == null) {
        event.setStatus(new TransactionStatus(Status.REJECTED, "Unknown Account"));
      } else {
        event.setStatus(new TransactionStatus(Status.REJECTED, "Inactive Account"));
      }
      return event;
    }).to(Topics.TRANSACTION_OUTPUT_TOPIC,
        Produced.with(Serdes.String(), jsonTransactionEventSerde));

    KStream<String, AccountBalance> balanceStream = branches[1].groupByKey()
        .aggregate(new Initializer<AccountBalance>() {
                     public AccountBalance apply() {
                       return new AccountBalance();
                     }
                   }, new Aggregator<String, Pair<TransactionEvent, Account>, AccountBalance>() {
                     @Override
                     public AccountBalance apply(String key, Pair<TransactionEvent, Account> value,
                         AccountBalance aggregate) {
                       String accountNr = value.getLeft().getAccountNr();

                       AccountBalance accountBalance =
                           aggregate.getAccountNr() == null ? new AccountBalance(accountNr,
                               BigDecimal.valueOf(0), null) : aggregate;
                       BigDecimal amount = value.getLeft().getAmount();
                       TransactionType transactionType = value.getLeft().getTransactionType();
                       if (isPaymentOverAvailableBalance(value.getLeft(), accountBalance)) {
                         value.getLeft().setStatus(
                             new TransactionStatus(Status.REJECTED, "Insufficient funds"));
                       } else {
                         accountBalance.setBalance(
                             (transactionType.equals(
                                 TransactionType.DEPOSIT)
                                 ? accountBalance.getBalance().add(amount)
                                 : accountBalance.getBalance()
                                     .subtract(amount)));
                         value.getLeft().setStatus(new TransactionStatus(Status.PROCESSED, ""));
                       }
                       accountBalance.setLastTransaction(value.getLeft());
                       return accountBalance;
                     }
                   }, Named.as("account-balances"),
            Materialized.with(Serdes.String(), jsonAccountBalanceSerde))
        .toStream();

    balanceStream.mapValues(AccountBalance::getLastTransaction)
        .to(Topics.TRANSACTION_OUTPUT_TOPIC,
            Produced.with(Serdes.String(), jsonTransactionEventSerde));
    balanceStream.to(Topics.BALANCE_UPDATES_TOPIC);

    final KafkaStreams streams = new KafkaStreams(builder.build(), props);
    streams.cleanUp();
    final CountDownLatch latch = new CountDownLatch(1);

    // attach shutdown handler to catch control-c
    Runtime.getRuntime().

        addShutdownHook(new Thread("streams-" + app + "-shutdown-hook") {
          @Override
          public void run() {
            streams.close();
            latch.countDown();
          }
        });

    try {
      streams.start();
      latch.await();
    } catch (
        final Throwable e) {
      e.printStackTrace();
      System.exit(1);
    }
    System.exit(0);
  }


  private static boolean isPaymentOverAvailableBalance(TransactionEvent left,
      AccountBalance right) {
    return (left.getTransactionType() == TransactionType.PAYMENT
        && left.getAmount().compareTo(right.getBalance()) > 0);
  }


  private static void deleteTopic(String topicName, AdminClient adminClient) {
    try {
      adminClient.deleteTopics(
          Collections.singleton(topicName)).all().get();
      Thread.sleep(500);
    } catch (Exception e) {
      log.warn("Error deleting topic ", e);
    }
  }

  private static void recreateTopicsFor2Clusters(Properties props) {
    Properties props1 = new Properties(props);
    props1.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
    recreateTopics(props1);
    Properties props2 = new Properties(props);
    props2.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER_2);
    recreateTopics(props2);
  }

  private static void recreateTopics(Properties props) {
    List<String> topics = Arrays.asList(
        Topics.ACCOUNT_IN,
        Topics.ACCOUNT_KTABLE,
        Topics.ACCOUNT_OUTPUT_TOPIC,
        Topics.TRANSACTION_IN,
        Topics.TRANSACTION_PROCESSING_TOPIC,
        Topics.TRANSACTION_OUTPUT_TOPIC,
        Topics.BALANCE_UPDATES_TOPIC
    );

    AdminClient kafkaAdminClient = AdminClient.create(props);

    var res = kafkaAdminClient.createTopics(
        topics.stream()
            .map(topic -> new NewTopic(topic, 2, (short) 1))
            .collect(Collectors.toList()));

    try {
      res.all().get();
    } catch (Exception e) {
      log.warn("Error creating topic ", e);
    }
    int count = 0;

    do {
      try {
        Thread.sleep(100);
        count = (int) kafkaAdminClient.listTopics().names().get().stream()
            .filter(topics::contains)
            .count();
      } catch (Exception e) {
        log.warn("Error creating topic ", e);
      }
    } while (count != topics.size());
  }
}
