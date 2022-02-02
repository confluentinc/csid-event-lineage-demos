package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.datainjector;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountEvent.AccountEventType;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.AccountHolder;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Address;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.Merchant;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionEvent.TransactionType;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus;
import io.confluent.csid.data.governance.lineage.opentel.transactiondemo.common.domain.TransactionStatus.Status;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

@Slf4j
public class DataInjector {

  private static final int MAX_CLOSED_ACCOUNT_PAYMENT_ATTEMPTS = 2;
  static List<String> usedAccNrs = new ArrayList<>();
  static List<String> testAccNrs = Arrays.asList(
      "1009991001",
      "1009991002",
      "1009991003",
      "1009991004",
      "1009991005",
      "1009991006",
      "1009991007",
      "1009991008",
      "1009991009",
      "1009991010"
  );

  static final int ACCOUNT_OPEN_FREQ_SECONDS = 10;
  static final int TRANSACTION_FREQ_MILLIS = 100;
  static final int PAYMENT_TO_DEPOSIT_RATIO = 10;
  static final int WITHDRAWAL_OVER_BALANCE_PERCENTAGE = 5;
  static final int INITIAL_OPEN_ACCOUNTS = 20;
  static final List<AccountEvent> ACCOUNT_OPEN_EVENTS = new ArrayList<>();
  static final List<Merchant> MERCHANTS = new ArrayList<>();
  static long LAST_ACCOUNT_OPEN;
  static long LAST_TRANSACTION;
  static long LAST_ACCOUNT_CLOSE;
  static long ACCOUNT_CLOSE_FREQ_SECONDS = 30;
  static int ACCOUNT_CLOSE_DELAY_SECONDS = 30;

  static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  static boolean SHUTDOWN_FLAG = false;


  @SneakyThrows
  public static void main(final String[] args) {
    int runTimeSeconds = tryParseParameter(args[0]);
    long startTime = System.currentTimeMillis();
    LAST_ACCOUNT_CLOSE = startTime + ACCOUNT_CLOSE_DELAY_SECONDS;
    FileReader fileReader = new FileReader();
    usedAccNrs.addAll(testAccNrs);

    MERCHANTS.addAll(Arrays.asList(OBJECT_MAPPER.readValue(
        fileReader.getFileFromResourceAsStream("MerchantData.json"), Merchant[].class)));

    ACCOUNT_OPEN_EVENTS.addAll(Arrays.asList(OBJECT_MAPPER.readValue(
        fileReader.getFileFromResourceAsStream("AccountOpenEventData.json"),
        AccountEvent[].class)));
    Map<String, AccountState> openAccountBalances = new HashMap<>();

    List<ActionExecutor> sendingPool = IntStream.range(0, 10)
        .mapToObj(i -> new ActionExecutor()).collect(
            Collectors.toList());

    sendingPool.forEach(ActionExecutor::start);
    openInitialAccounts(sendingPool, openAccountBalances);
    LAST_ACCOUNT_OPEN = System.currentTimeMillis();
    while (!SHUTDOWN_FLAG) {
      if (runTimeSeconds > 0) {
        if (System.currentTimeMillis() > (startTime + (long) runTimeSeconds * 1000)) {
          System.exit(0);
        }
      }
      scheduleNextOp(sendingPool, openAccountBalances, startTime);
    }
  }

  private static int tryParseParameter(String param) {
    int res = 0;
    if (param != null && param.length() > 0) {
      try {
        res = Integer.parseInt(param);
      } catch (NumberFormatException e) {
        log.warn("Failed to parse input parameter - expecting Integer", e);
      }
    }
    return res;
  }

  private static void scheduleNextOp(List<ActionExecutor> sendingPool,
      Map<String, AccountState> openAccountBalances, long startTime) {
    long now = System.currentTimeMillis();
    if (now - (ACCOUNT_OPEN_FREQ_SECONDS * 1000) >= LAST_ACCOUNT_OPEN) {
      openRandomNewAccount(sendingPool, openAccountBalances);
      LAST_ACCOUNT_OPEN = now;
    }
    if (now - TRANSACTION_FREQ_MILLIS >= LAST_TRANSACTION) {
      performTransactionForRandomAccount(sendingPool, openAccountBalances);
      LAST_TRANSACTION = now;
    }
    if (now - (ACCOUNT_CLOSE_FREQ_SECONDS * 1000) >= LAST_ACCOUNT_CLOSE) {
      closeRandomAccount(sendingPool, openAccountBalances);
      LAST_ACCOUNT_CLOSE = now;
    }
    Thread.yield();
  }

  private static void closeRandomAccount(List<ActionExecutor> sendingPool,
      Map<String, AccountState> openAccountBalances) {
    List<AccountState> openAccounts = openAccountBalances.values().stream()
        .filter(
            accountState -> accountState.isOpen)
        .collect(
            Collectors.toList());
    if (openAccounts.size() == 0) {
      return;
    }
    AccountState account = openAccounts.get(RandomUtils.nextInt(0, openAccounts.size()));

    Consumer<HttpClient> action = closeAccountAction(new AccountEvent(account.getAccountNr(),
        AccountEventType.CLOSE, null));
    pushToExecutors(new AccountAction(account.getAccountNr(), action), sendingPool);
  }

  private static void performTransactionForRandomAccount(
      List<ActionExecutor> sendingPool, Map<String, AccountState> openAccountBalances) {
    long now = System.currentTimeMillis();
    List<AccountState> openAccounts = openAccountBalances.values().stream()
        .filter(
            accountState -> accountState.isOpen && accountState.earliestNextActionTimestamp < now)
        .collect(
            Collectors.toList());
    if (openAccounts.size() == 0) {
      return;
    }
    AccountState account = openAccounts.get(RandomUtils.nextInt(0, openAccounts.size()));
    int withdrawalWithinBalanceChance = 100 - WITHDRAWAL_OVER_BALANCE_PERCENTAGE;
    boolean doFailedWithdrawal = RandomUtils.nextInt(0, 100) >= withdrawalWithinBalanceChance;
    double amount;
    if (doFailedWithdrawal) {
      amount = round(0 - account.balance.doubleValue() - RandomUtils.nextDouble(0.01, 1000), 2);
    } else if (account.balance.doubleValue() < 50
        || RandomUtils.nextInt(0, PAYMENT_TO_DEPOSIT_RATIO + account.numberConsecutivePayments) >= (
        PAYMENT_TO_DEPOSIT_RATIO - 1)) {
      //deposit
      amount = round(RandomUtils.nextDouble(100, 2000), 2);
    } else {
      amount = round(0 - RandomUtils.nextDouble(0.01, account.balance.doubleValue()), 2);
      if (Math.abs(account.balance.doubleValue() + amount) < 1) {
        amount = 0 - account.balance.doubleValue();
      }
    }
    scheduleTransaction(account.accountNr, openAccountBalances, sendingPool,
        BigDecimal.valueOf(amount));
  }

  public static double round(double value, int places) {
    if (places < 0) {
      throw new IllegalArgumentException();
    }

    BigDecimal bd = BigDecimal.valueOf(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }

  private static void openRandomNewAccount(List<ActionExecutor> sendingPool,
      Map<String, AccountState> openAccountBalances) {
    while (true) {
      AccountEvent accountEvent = ACCOUNT_OPEN_EVENTS.get(
          RandomUtils.nextInt(0, ACCOUNT_OPEN_EVENTS.size()));
      if (openAccountBalances.containsKey(accountEvent.getAccountNr())) {
        continue;
      }
      scheduleAccountOpen(accountEvent.getAccountNr(), openAccountBalances, sendingPool);
      break;
    }
  }


  private static void openInitialAccounts(
      List<ActionExecutor> sendingPool,
      Map<String, AccountState> openAccountBalances) {
    IntStream.range(0, 5)
        .forEach(
            i -> scheduleAccountOpen(testAccNrs.get(i), openAccountBalances, sendingPool));
    IntStream.range(0, INITIAL_OPEN_ACCOUNTS)
        .forEach(
            i -> openRandomNewAccount(sendingPool, openAccountBalances));
  }

  private static void scheduleAccountOpen(String accountNr,
      Map<String, AccountState> openAccountBalances,
      List<ActionExecutor> sendingPool) {
    Consumer<HttpClient> action = openAccountAction(ACCOUNT_OPEN_EVENTS.stream()
        .filter(accountEvent -> accountEvent.getAccountNr().equals(accountNr)).findFirst().get());
    openAccountBalances.put(accountNr, new AccountState(accountNr));
    pushToExecutors(new AccountAction(accountNr, action), sendingPool);

  }

  private static void scheduleTransaction(String accountNr,
      Map<String, AccountState> openAccountBalances,
      List<ActionExecutor> sendingPool, BigDecimal amount) {

    Consumer<HttpClient> action = transferAction(accountNr, amount);
    AccountState accountState = openAccountBalances.get(accountNr);
    if (amount.doubleValue() < 0) {
      accountState.addPayment(amount.abs());
    } else {
      accountState.addDeposit(amount.abs());
    }
    if (!accountState.isOpen) {
      accountState.recordClosedPaymentAttempt();
    }
    if (accountState.numberPaymentAttemptsAfterClosed >= MAX_CLOSED_ACCOUNT_PAYMENT_ATTEMPTS) {
      openAccountBalances.remove(accountNr);
    }
    pushToExecutors(new AccountAction(accountNr, action), sendingPool);
  }

  private static void pushToExecutors(AccountAction nextAction, List<ActionExecutor> sendingPool) {
    int executorIndex = Math.abs(nextAction.accountNr.hashCode()) % 9;
    sendingPool.get(executorIndex).addAction(nextAction.action);
  }

  private static Consumer<HttpClient> transferAction(String accountNr, BigDecimal amount) {
    return (client -> {
      try {
        TransactionEvent transactionEvent = new TransactionEvent(UUID.randomUUID().toString(),
            accountNr,
            amount.abs(),
            amount.doubleValue() > 0 ? null : randomMerchant(),
            amount.doubleValue() > 0 ? TransactionType.DEPOSIT : TransactionType.PAYMENT,
            TransactionStatus.builder().status(Status.PENDING).build());
        String json = OBJECT_MAPPER.writeValueAsString(transactionEvent);
        log.info("Sending transaction event: {}", json);
        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7071/produce-transaction-event"))
                .POST(
                    BodyPublishers.ofString(json)).build(), BodyHandlers.ofString());
        log.info("Response for transaction event: {}", response);
      } catch (InterruptedException | IOException e) {
        log.warn("Exception handling transaction send. Exception: ", e);
      }
    });
  }

  private static Merchant randomMerchant() {
    return MERCHANTS.get(RandomUtils.nextInt(0, MERCHANTS.size()));
  }

  private static Consumer<HttpClient> openAccountAction(AccountEvent accountEvent) {
    return (client -> {
      try {
        String json = OBJECT_MAPPER.writeValueAsString(accountEvent);
        log.info("Sending account open event: {}", json);

        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:7070/produce-account-event"))
                .POST(
                    BodyPublishers.ofString(json)).build(), BodyHandlers.ofString());
        log.info("Response for account event: {}", response);
      } catch (InterruptedException | IOException e) {
        log.warn("Exception handling open account send. Exception: ", e);
      }
    });
  }

  private static Consumer<HttpClient> closeAccountAction(AccountEvent accountEvent) {
    return (client -> {
      try {
        String json = OBJECT_MAPPER.writeValueAsString(accountEvent);
        log.info("Sending account close event: {}", json);

        HttpResponse<String> response = client.send(
            HttpRequest.newBuilder().uri(URI.create("http://localhost:7070/produce-account-event"))
                .POST(
                    BodyPublishers.ofString(json)).build(), BodyHandlers.ofString());
        log.info("Response for account event: {}", response);
      } catch (InterruptedException | IOException e) {
        log.warn("Exception handling close account send. Exception: ", e);
      }
    });
  }

}
