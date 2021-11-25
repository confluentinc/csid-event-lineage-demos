package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.demodata;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Address;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Card;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.ContactDetails;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Location;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Merchant;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Payee;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ScenarioData {
    final Address payeeAddress;
    final ContactDetails payeeContactDetails;
    final Payee payee;
    final Card card;
    final Address merchantAddress;
    final Location merchantLocation;
    final Merchant merchant;
    final Location transactionLocation;
    Double amount;
    final String transactionId;
}