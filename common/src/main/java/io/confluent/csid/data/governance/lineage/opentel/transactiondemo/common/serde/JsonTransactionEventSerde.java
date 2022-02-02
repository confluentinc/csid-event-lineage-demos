package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent;
import java.io.IOException;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class JsonTransactionEventSerde implements Serializer<TransactionEvent>,
    Deserializer<TransactionEvent>, Serde<TransactionEvent> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
  }

  @SuppressWarnings("unchecked")
  @Override
  public TransactionEvent deserialize(final String topic, final byte[] data) {
    if (data == null) {
      return null;
    }

    try {
      return OBJECT_MAPPER.readValue(data, TransactionEvent.class);
    } catch (final IOException e) {
      throw new SerializationException(e);
    }
  }

  @Override
  public byte[] serialize(final String topic, final TransactionEvent data) {
    if (data == null) {
      return null;
    }

    try {
      return OBJECT_MAPPER.writeValueAsBytes(data);
    } catch (final Exception e) {
      throw new SerializationException("Error serializing JSON message", e);
    }
  }

  @Override
  public void close() {
  }

  @Override
  public Serializer<TransactionEvent> serializer() {
    return this;
  }

  @Override
  public Deserializer<TransactionEvent> deserializer() {
    return this;
  }
}


