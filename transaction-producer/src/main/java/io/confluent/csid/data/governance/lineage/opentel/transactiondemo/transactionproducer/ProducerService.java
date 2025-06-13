package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.transactionproducer;

import static io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.BOOTSTRAP_KAFKA_SERVER;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.Constants.Topics;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonTransactionEventSerde;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.StringSerializer;

@Slf4j
public class ProducerService implements AutoCloseable {

  private final KafkaProducer<String, TransactionEvent> kafkaProducer;

  public ProducerService() {
    this.kafkaProducer = new KafkaProducer<>(properties());
  }

  private static Properties properties() {
    Properties props = new Properties();
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonTransactionEventSerde.class);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "Transaction-Event-Producer");
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_KAFKA_SERVER);
    
    // Add reliability configurations
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 3);
    props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    
    return props;
  }

  public void produce(TransactionEvent data) {
    try {
      RecordHeaders recordHeaders = new RecordHeaders();
      recordHeaders.add(new RecordHeader("account_nr_header", 
          data.getAccountNr().getBytes(StandardCharsets.UTF_8)));
      
      ProducerRecord<String, TransactionEvent> record = new ProducerRecord<>(
          Topics.TRANSACTION_IN, 
          null, 
          null, 
          data.getAccountNr(), 
          data,
          recordHeaders
      );

      Future<RecordMetadata> future = kafkaProducer.send(record);
      RecordMetadata metadata = future.get(); // Wait for the send to complete
      
      log.info("Produced transaction event with key={}, value={}, partition={}, offset={}", 
          data.getAccountNr(), 
          data,
          metadata.partition(),
          metadata.offset());
          
    } catch (InterruptedException | ExecutionException e) {
      log.error("Failed to produce transaction event: {}", data, e);
      Thread.currentThread().interrupt();
      throw new RuntimeException("Failed to produce transaction event", e);
    }
  }

  @Override
  public void close() {
    try {
      kafkaProducer.flush();
      kafkaProducer.close();
      log.info("Kafka producer closed successfully");
    } catch (Exception e) {
      log.error("Error closing Kafka producer", e);
    }
  }
}
