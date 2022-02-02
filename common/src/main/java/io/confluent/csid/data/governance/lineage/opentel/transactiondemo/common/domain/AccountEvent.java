package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountEvent {

  String accountNr;
  AccountEventType accountEventType;
  AccountHolder accountHolder;
  public enum AccountEventType {
    OPEN,
    CLOSE
  }
}
