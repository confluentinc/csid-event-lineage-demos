package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

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
public class Transaction {
    String transactionId;
    Card card;
    Merchant merchant;
    Payee payee;
    Location location;
    Double amount;
    TransactionStatus status;
}
