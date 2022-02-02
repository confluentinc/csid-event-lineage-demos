package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.accountproducer;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.serde.JsonAccountEventSerde;
import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountEventService {

  /*
  key - account number,
  value - Account event - Open or Close
   */
  public static void main(final String[] args) {
    ProducerService producerService = new ProducerService();
    JsonAccountEventSerde jsonAccountEventSerde = new JsonAccountEventSerde();
    Javalin app = Javalin.create(config -> {
      config.enforceSsl = false;
      config.asyncRequestTimeout = 10_000L;

    }).routes(() -> {
      path("produce-account-event", () ->
          post(ctx -> {
            log.info("Account Event: {}", new String(ctx.bodyAsBytes()));
            producerService.produce(jsonAccountEventSerde.deserialize("", ctx.bodyAsBytes()));
            ctx.result("OK");
          })
      );
    });

    Runtime.getRuntime().addShutdownHook(new Thread("account-producer-shutdown-hook") {
      @Override
      public void run() {
        producerService.close();
        app.close();

      }
    });
    app.start(7070);
  }
}
