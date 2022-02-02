package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.balanceupdatessink;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.Topics;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Account;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountBalance;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonAccountBalanceSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonTransactionEventSerde;

import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class BalanceUpdatesSink {

    public static boolean stop = false;

    public static void main(final String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread("balance-updates-consumer-shutdown-hook") {
            @Override
            public void run() {
                stop = true;
            }
        });
        KafkaConsumer<String, AccountBalance> kafkaConsumer = new KafkaConsumer<>(properties());
        kafkaConsumer.subscribe(Collections.singleton(Topics.BALANCE_UPDATES_TOPIC));
        while (!stop) {
            ConsumerRecords<String, AccountBalance> records = kafkaConsumer.poll(Duration.ofSeconds(2));
            if (!records.isEmpty()) {
                Iterator<ConsumerRecord<String, AccountBalance>> iterator = records.iterator();
                while (iterator.hasNext()) {
                    ConsumerRecord<String, AccountBalance> record = iterator.next();
                    System.out.println("Consumed - Key:" + record.key() + ", Value:" + record.value().toString());
                }
            }
        }

        kafkaConsumer.close();
        System.exit(0);
    }

    private static Properties properties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonAccountBalanceSerde.class);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "BalanceUpdatesConsumer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "BalanceUpdatesConsumer");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
        return props;
    }
}
