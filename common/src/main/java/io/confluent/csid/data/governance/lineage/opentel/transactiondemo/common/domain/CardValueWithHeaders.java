package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

import java.util.Map.Entry;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardValueWithHeaders {
    Set<Entry<String, byte[]>> headers;
    Card value;
}
