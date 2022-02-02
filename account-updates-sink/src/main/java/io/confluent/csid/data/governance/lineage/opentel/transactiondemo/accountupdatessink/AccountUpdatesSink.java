package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.accountupdatessink;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.Topics;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Account;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonAccountSerde;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class AccountUpdatesSink {

  public static boolean stop = false;

  public static void main(final String[] args) {
    Runtime.getRuntime().addShutdownHook(new Thread("account-updates-consumer-shutdown-hook") {
      @Override
      public void run() {
        stop = true;
      }
    });
    KafkaConsumer<String, Account> kafkaConsumer = new KafkaConsumer<>(properties());
    kafkaConsumer.subscribe(Collections.singleton(Topics.ACCOUNT_OUTPUT_TOPIC));
    while (!stop) {
      ConsumerRecords<String, Account> records = kafkaConsumer.poll(Duration.ofSeconds(2));
      if (!records.isEmpty()) {
        Iterator<ConsumerRecord<String, Account>> iterator = records.iterator();
        while (iterator.hasNext()) {
          ConsumerRecord<String, Account> record = iterator.next();
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
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonAccountSerde.class);
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, "AccountUpdatesConsumer");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "AccountUpdatesConsumer");
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
    return props;
  }
}
