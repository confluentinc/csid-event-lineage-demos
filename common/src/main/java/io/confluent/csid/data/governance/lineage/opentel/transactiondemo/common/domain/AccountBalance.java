package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {
  String accountNr;
  BigDecimal balance;
  TransactionEvent lastTransaction;
}
