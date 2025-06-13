package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.transactionproducer;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonTransactionEventSerde;
import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

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
      config.http.asyncTimeout = 10_000L;
      config.router.apiBuilder(() -> {
        path("produce-transaction-event", () -> {
          post(ctx -> {
            log.info("Transaction Event: {}", new String(ctx.bodyAsBytes()));
            producerService.produce(jsonTransactionEventSerde.deserialize("", ctx.bodyAsBytes()));
            ctx.result("OK");
          });
        });
      });
    }).start(7071);

    Runtime.getRuntime().addShutdownHook(new Thread("transaction-producer-shutdown-hook") {
      @Override
      public void run() {
        producerService.close();
        app.stop();
      }
    });
  }
}
