package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.datainjector;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ActionExecutor {

  ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final HttpClient httpClient = buildHttpClient();
  private final LinkedBlockingQueue<Consumer<HttpClient>> actions = new LinkedBlockingQueue<>();
  boolean stop = false;

  void start() {
    executorService.submit(() -> {
      while (!stop) {
        try {
         actions.poll(10, TimeUnit.MINUTES).accept(httpClient);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          stop = true;
          return;
        }
      }
    });
  }

  public void addAction(Consumer<HttpClient> action) {
    try {
      actions.put(action);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      stop = true;
    }
  }

  private HttpClient buildHttpClient() {
    return HttpClient.newBuilder().version(Version.HTTP_2)
        .build();
  }
}

