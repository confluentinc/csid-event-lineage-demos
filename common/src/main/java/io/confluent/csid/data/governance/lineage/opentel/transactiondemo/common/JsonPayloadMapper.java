package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class JsonPayloadMapper {
    static ObjectMapper objectMapper = new ObjectMapper();
    @SneakyThrows
    public static String mapToJson(Object object){
        return objectMapper.writeValueAsString(object);
    }
}
