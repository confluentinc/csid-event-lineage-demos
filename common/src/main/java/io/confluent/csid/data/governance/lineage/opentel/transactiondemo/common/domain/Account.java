package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Account {

  String accountNr;
  AccountHolder accountHolder;
  boolean active;
}
