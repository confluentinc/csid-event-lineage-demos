package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.transactionsink;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.INPUT_TOPIC;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.TRANSACTION_OUTPUT_TOPIC;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.JsonTransactionSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Transaction;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;

public class TransactionSink {

    public static boolean stop = false;

    public static void main(final String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread("transaction-consumer-shutdown-hook") {
            @Override
            public void run() {
                stop = true;
            }
        });
        KafkaConsumer<Long, Transaction> kafkaConsumer = new KafkaConsumer<>(properties());
        kafkaConsumer.subscribe(Collections.singleton(INPUT_TOPIC));
        while (!stop) {
            ConsumerRecords<Long, Transaction> records = kafkaConsumer.poll(Duration.ofSeconds(2));
            if (!records.isEmpty()) {
                Iterator<ConsumerRecord<Long, Transaction>> iterator = records.iterator();
                while (iterator.hasNext()) {
                    ConsumerRecord<Long, Transaction> record = iterator.next();
                    System.out.println("Consumed - Key:" + record.key().toString() + ", Value:" + record.value().toString());
                }
            }
        }

        kafkaConsumer.close();
        System.exit(0);
    }

    private static Properties properties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonTransactionSerde.class);
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, "TransactionConsumer");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "TransactionConsumer");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
        return props;
    }
}
