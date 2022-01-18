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

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BALANCE_VERIFICATION_TOPIC;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.CARD_DETAILS_TOPIC;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.ENTITY_ENRICHMENTS_TOPIC;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.FRAUD_DETECTION_TOPIC;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.INPUT_TOPIC;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.TRANSACTION_OUTPUT_TOPIC;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.TRANSACTION_PROCESSING_TOPIC;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.JsonCardSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.JsonTransactionSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.demodata.DemoData;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.demodata.ScenarioData;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Card;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Transaction;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus.Status;
import io.opentelemetry.context.propagation.TextMapGetter;
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
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serdes.LongSerde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.ValueJoiner;

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

  public static void main(final String[] args) {
    String app = System.getProperty("app");
    final Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, app);
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
    props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, LongSerde.class);
    props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonTransactionSerde.class);
    props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
    props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000L);

    // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    final StreamsBuilder builder = new StreamsBuilder();
    switch (app) {
      case "card-enrichment":
        recreateTopics(props);
        builder.stream(INPUT_TOPIC, Consumed.with(Serdes.Long(), new JsonTransactionSerde()))
            .mapValues(v -> {
              v.setCard(DemoData.getScenarioData().get(v.getTransactionId()).getCard());
              return v;
            }).to(BALANCE_VERIFICATION_TOPIC);
        break;
      case "balance-verification":
        builder.stream(BALANCE_VERIFICATION_TOPIC,
                Consumed.with(Serdes.Long(), new JsonTransactionSerde())).split()
            .branch(((key, value) -> value.getAmount() <= value.getCard().getAvailableBalance()),
                Branched.withConsumer(x -> x.to(ENTITY_ENRICHMENTS_TOPIC)))
            .defaultBranch(Branched.withConsumer(x -> x.mapValues(v -> {
              v.setStatus(new TransactionStatus(Status.REJECTED, "Insufficient balance"));
              return v;
            }).to(TRANSACTION_OUTPUT_TOPIC)));
        break;
      case "entity-enrichment":
        builder.stream(ENTITY_ENRICHMENTS_TOPIC,
                Consumed.with(Serdes.Long(), new JsonTransactionSerde()))
            .mapValues(v -> {
              ScenarioData scenarioData = DemoData.getScenarioData().get(v.getTransactionId());
              v.setPayee(scenarioData.getPayee());
              v.setMerchant(scenarioData.getMerchant());
              return v;
            }).to(FRAUD_DETECTION_TOPIC);
        break;

      case "fraud-detection":
        builder.stream(FRAUD_DETECTION_TOPIC,
                Consumed.with(Serdes.Long(), new JsonTransactionSerde()))
            .mapValues(v -> {
              if (!isCountryAuthorized(v)) {
                v.setStatus(new TransactionStatus(Status.REJECTED,
                    "Transaction country not authorized for card"));
              } else if (!isLocationInVicinity(v)) {
                v.setStatus(new TransactionStatus(Status.REJECTED,
                    "Transaction location does not match Merchant location"));
              }
              return v;
            })
            .split()
            .branch((key, value) -> Status.REJECTED == value.getStatus().getStatus(),
                Branched.withConsumer(x -> x.to(TRANSACTION_OUTPUT_TOPIC)))
            .defaultBranch(Branched.withConsumer(x -> x.to(TRANSACTION_PROCESSING_TOPIC)));
        break;

      case "transaction-processor":
        builder.stream(TRANSACTION_PROCESSING_TOPIC,
                Consumed.with(Serdes.Long(), new JsonTransactionSerde()))
            .mapValues(v -> {
              v.setStatus(new TransactionStatus(Status.PROCESSED, null));
              return v;
            }).to(TRANSACTION_OUTPUT_TOPIC);
        break;
      case "card-enrichment-ktable":
        recreateTopics(props);
        KTable<Long, Card> cardDetails = builder.stream(CARD_DETAILS_TOPIC,
                Consumed.with(Serdes.Long(), new JsonCardSerde()))
            .toTable(Materialized.with(Serdes.Long(), new JsonCardSerde()));
        builder.stream(INPUT_TOPIC, Consumed.with(Serdes.Long(), new JsonTransactionSerde()))
            .leftJoin(cardDetails,
                new ValueJoiner<Transaction, Card, Object>() {
                  @Override
                  public Object apply(final Transaction value1, final Card value2) {
                    value1.setCard(value2);
                    return value1;
                  }
                }
            ).to(BALANCE_VERIFICATION_TOPIC);
        break;
      default:
        throw new IllegalArgumentException(
            "Application type not recognized - only accepted values are :"
                + " card-details-enrich, balance-verification, entity-enrichment, fraud-detection, transaction-processor)");
    }

    final KafkaStreams streams = new KafkaStreams(builder.build(), props);
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

  private static boolean isLocationInVicinity(final Transaction transaction) {
    double threshold = 1;

    return
        (Math.abs(transaction.getLocation().getLatitude() - transaction.getMerchant().getLocation()
            .getLatitude()) < threshold)
            &&
            (Math.abs(
                transaction.getLocation().getLongitude() - transaction.getMerchant().getLocation()
                    .getLongitude()) < threshold);
  }

  private static boolean isCountryAuthorized(Transaction transaction) {
    return transaction.getCard().getAuthorizedCountries()
        .contains(transaction.getMerchant().getAddress().getCountry());
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

  private static void recreateTopics(Properties props) {
    List<String> topics = Arrays.asList(
        INPUT_TOPIC,
        BALANCE_VERIFICATION_TOPIC,
        ENTITY_ENRICHMENTS_TOPIC,
        FRAUD_DETECTION_TOPIC,
        TRANSACTION_PROCESSING_TOPIC,
        TRANSACTION_OUTPUT_TOPIC,
        CARD_DETAILS_TOPIC
    );

    AdminClient kafkaAdminClient = AdminClient.create(props);
         /*
         topics.forEach(topic->deleteTopic(topic, kafkaAdminClient));

        int count = 0;
        do {
            try {
                Thread.sleep(100);
                count = (int) kafkaAdminClient.listTopics().names().get().stream().filter(topics::contains).count();
            } catch (Exception e) {
                log.warn("Error deleting topic ", e);
            }
        } while (count > 0);
*/
    var res = kafkaAdminClient.createTopics(
        topics.stream()
            .map(topic -> new NewTopic(topic, 1, (short) 1))
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
        count = (int) kafkaAdminClient.listTopics().names().get().stream().filter(topics::contains)
            .count();
      } catch (Exception e) {
        log.warn("Error creating topic ", e);
      }
    } while (count != topics.size());
  }
}
