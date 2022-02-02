/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Account;
import java.io.IOException;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class JsonAccountSerde implements Serializer<Account>, Deserializer<Account>,
    Serde<Account> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
  }

  @SuppressWarnings("unchecked")
  @Override
  public Account deserialize(final String topic, final byte[] data) {
    if (data == null) {
      return null;
    }

    try {
      return OBJECT_MAPPER.readValue(data, Account.class);
    } catch (final IOException e) {
      throw new SerializationException(e);
    }
  }

  @Override
  public byte[] serialize(final String topic, final Account data) {
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
  public Serializer<Account> serializer() {
    return this;
  }

  @Override
  public Deserializer<Account> deserializer() {
    return this;
  }
}


