package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Data
public class TransactionStatus {
    Status status;
    String reason;
    public enum Status {
        PROCESSED,
        REJECTED,
        PENDING
    }
}
