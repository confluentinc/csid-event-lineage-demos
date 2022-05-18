package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.transactionproducer;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.Topics;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonTransactionEventSerde;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class ProducerService {

  KafkaProducer<String, TransactionEvent> kafkaProducer = new KafkaProducer<>(properties());

  private static Properties properties() {
    Properties props = new Properties();
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonTransactionEventSerde.class);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "Transaction-Event-Producer");
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
    return props;
  }

  public void produce(TransactionEvent data) {
    RecordHeaders recordHeaders = new RecordHeaders();
    recordHeaders.add(new RecordHeader("account_nr_header", data.getAccountNr().getBytes(
        StandardCharsets.UTF_8)));
    kafkaProducer.send(
        new ProducerRecord<>(Topics.TRANSACTION_IN, null, null, data.getAccountNr(), data,
            recordHeaders));
    kafkaProducer.flush();
    log.info("Produced transaction event with key={}, value={}", data.getAccountNr(), data);

  }

  public void close() {
    kafkaProducer.close();
  }
}
