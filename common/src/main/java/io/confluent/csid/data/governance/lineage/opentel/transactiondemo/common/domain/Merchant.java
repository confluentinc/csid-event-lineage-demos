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
public class Merchant {
    String id;
    Address address;
    Location location;
    String bank;
    Integer accountNumber;
}
