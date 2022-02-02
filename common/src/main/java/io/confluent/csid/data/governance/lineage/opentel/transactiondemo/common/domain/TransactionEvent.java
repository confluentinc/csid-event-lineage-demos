package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionEvent {
    String transactionId;
    String accountNr;
    BigDecimal amount;
    Merchant merchant;
    TransactionType transactionType;
    TransactionStatus status;

    public enum TransactionType{
        PAYMENT,
        DEPOSIT
    }
}
