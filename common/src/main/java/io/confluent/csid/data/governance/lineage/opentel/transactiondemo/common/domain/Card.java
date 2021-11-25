package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain;

import java.util.Date;
import java.util.List;
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
public class Card {
    Long cardNumber;
    Integer cvv;
    String expiry;
    Integer availableBalance;
    String bank;
    Integer accountNumber;
    List<String> authorizedCountries;
}
