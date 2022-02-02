package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.accountproducer;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.Topics;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonAccountEventSerde;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class ProducerService {

  KafkaProducer<String, AccountEvent> kafkaProducer = new KafkaProducer<>(properties());

  private static Properties properties() {
    Properties props = new Properties();
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonAccountEventSerde.class);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "Account-Event-Producer");
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
    return props;
  }

  public void produce(AccountEvent data) {
    kafkaProducer.send(new ProducerRecord<>(Topics.ACCOUNT_IN, data.getAccountNr(), data));
    kafkaProducer.flush();
    log.info("Produced account event with key={}, value={}", data.getAccountNr(), data);

  }

  public void close() {
    kafkaProducer.close();
  }
}
