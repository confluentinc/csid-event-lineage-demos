package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Span;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonSpansSerde implements Serializer<List<Span>>,
        Deserializer<List<Span>>, Serde<List<Span>> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Span> deserialize(final String topic, final byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            return OBJECT_MAPPER.readValue(data, List.class);
        } catch (final IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public Serializer<List<Span>> serializer() {
        return this;
    }

    @Override
    public Deserializer<List<Span>> deserializer() {
        return this;
    }

    @Override
    public byte[] serialize(final String topic, final List<Span> data) {
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
}
