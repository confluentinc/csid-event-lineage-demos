package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.transactionproducer;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.CARD_DETAILS_TOPIC;
import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.INPUT_TOPIC;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.JsonCardSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.JsonTransactionSerde;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.demodata.DemoData;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Card;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Transaction;
import java.util.Properties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class Producer {

    private static boolean stop;

    /*
    key - card number,
    value - Transaction with only minimum data populated (datetime, payee - id, card - number, cvv, expiry, merchant - id, location of operation)
     */
    public static void main(final String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread("producer-shutdown-hook") {
            @Override
            public void run() {
                stop = true;
            }
        });
        produceCardDetails();
        KafkaProducer<Long, Transaction> kafkaProducer = new KafkaProducer<>(properties());
        DemoData.getScenarioData()
            .forEach((transactionId, data) -> send(record(data.getCard().getCardNumber(), DemoData.getInitialTransaction(data)), kafkaProducer));
        while (!stop) {
            sleep(50);
            Thread.yield();
        }
        kafkaProducer.close();
    }

    private static void produceCardDetails() {
        Properties properties = properties();
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaProducer<Long, String> kafkaProducer = new KafkaProducer<>(properties);
        String cardDetails="{\n"
            + "    \"cardNumber\": 4624365230583743,\n"
            + "    \"cvv\": 732,\n"
            + "    \"expiry\": \"01/05/2023\",\n"
            + "    \"availableBalance\": 1350,\n"
            + "    \"bank\": \"Postbank\",\n"
            + "    \"accountNumber\": 382912341,\n"
            + "    \"authorizedCountries\": [\n"
            + "        \"Germany\",\n"
            + "        \"Austria\"\n"
            + "    ]\n"
            + "}";
            kafkaProducer.send(new ProducerRecord<>(CARD_DETAILS_TOPIC, 4624365230583743L, cardDetails));
            kafkaProducer.flush();

        sleep(500);
        kafkaProducer.close();
    }

    @SneakyThrows
    private static void sleep(long ms) {
        Thread.sleep(ms);
    }

    //@WithSpan(value = "PageViewProducerSend", kind = SpanKind.PRODUCER)
    private static void send(ProducerRecord<Long, Transaction> record, KafkaProducer<Long, Transaction> kafkaProducer) {
        //    Span.current().setAttribute("recordKey", record.key());
        //    Span.current().setAttribute("recordValue", record.value());
        kafkaProducer.send(record);
        kafkaProducer.flush();
        log.info("Produced key={}, value={}", record.key(), record.value());
    }

    private static ProducerRecord<Long, Transaction> record(long cardNumber, Transaction transaction) {
        return new ProducerRecord<>(INPUT_TOPIC, cardNumber, transaction);
    }

    private static Properties properties() {
        Properties props = new Properties();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonTransactionSerde.class);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "Producer");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
        return props;
    }
}
