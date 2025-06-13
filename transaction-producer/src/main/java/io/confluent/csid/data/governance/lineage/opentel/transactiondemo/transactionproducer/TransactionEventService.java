package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.transactionproducer;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonTransactionEventSerde;
import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionEventService {

  /*
  key - account number,
  value - Transaction event - Payment or Deposit
   */
  public static void main(final String[] args) {
    ProducerService producerService = new ProducerService();
    JsonTransactionEventSerde jsonTransactionEventSerde = new JsonTransactionEventSerde();
    
    Javalin app = Javalin.create(config -> {
      config.showJavalinBanner = false;
    });

    app.post("produce-transaction-event", ctx -> {
      log.info("Transaction Event: {}", new String(ctx.bodyAsBytes()));
      producerService.produce(jsonTransactionEventSerde.deserialize("", ctx.bodyAsBytes()));
      ctx.result("OK");
    });

    Runtime.getRuntime().addShutdownHook(new Thread("transaction-producer-shutdown-hook") {
      @Override
      public void run() {
        producerService.close();
        app.stop();
      }
    });
    app.start(7071);
  }
}
