package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.datainjector;

import java.net.http.HttpClient;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AccountAction {

  private String accountNr;
  private Consumer<HttpClient> action;

}

