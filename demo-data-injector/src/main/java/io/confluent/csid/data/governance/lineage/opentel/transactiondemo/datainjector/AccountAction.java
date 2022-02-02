package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.datainjector;

import java.net.http.HttpClient;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AccountAction {

  String accountNr;
  Consumer<HttpClient> action;

}

