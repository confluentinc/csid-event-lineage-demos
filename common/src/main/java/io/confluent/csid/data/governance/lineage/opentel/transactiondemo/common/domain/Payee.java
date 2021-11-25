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
public class Payee {
    String firstName;
    String lastName;
    Address address;
    String gender;
    ContactDetails contactDetails;
}
