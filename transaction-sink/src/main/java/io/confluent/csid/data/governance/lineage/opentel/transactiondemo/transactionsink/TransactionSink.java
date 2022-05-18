package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.transactionsink;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.Topics;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonTransactionEventSerde;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

public class TransactionSink {

  public static boolean stop = false;

  public static void main(final String[] args) {
    Runtime.getRuntime().addShutdownHook(new Thread("transaction-consumer-shutdown-hook") {
      @Override
      public void run() {
        stop = true;
      }
    });
    KafkaConsumer<String, TransactionEvent> kafkaConsumer = new KafkaConsumer<>(properties());
    kafkaConsumer.subscribe(Collections.singleton(Topics.TRANSACTION_OUTPUT_TOPIC));
    while (!stop) {
      ConsumerRecords<String, TransactionEvent> records = kafkaConsumer.poll(Duration.ofSeconds(2));
      if (!records.isEmpty()) {
        List<ConsumerRecord<String, TransactionEvent>> list = records.records(
            new TopicPartition(Topics.TRANSACTION_OUTPUT_TOPIC, 0));
        for (ConsumerRecord<String, TransactionEvent> record : list) {
          System.out.println(
              "Consumed - Key:" + record.key() + ", Value:" + record.value().toString());
        }
      }
    }
    kafkaConsumer.close();
    System.exit(0);
  }

  private static Properties properties() {
    Properties props = new Properties();
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonTransactionEventSerde.class);
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "TransactionConsumer-13");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "TransactionConsumer-13");
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return props;
  }
}
