package io.confluent.csid.data.governance.lineage.opentel.transactiondemo.datainjector;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AccountState {
  private static final long DELAY_AFTER_ACCOUNT_OPEN = 200L;
  private static final long DELAY_AFTER_TRANSACTION = 100L;

  String accountNr;
  BigDecimal balance;
  boolean isOpen;
  int numberConsecutivePayments;
  long earliestNextActionTimestamp;
  int numberPaymentAttemptsAfterClosed;

  public AccountState(String accountNr) {
    this.accountNr = accountNr;
    earliestNextActionTimestamp = System.currentTimeMillis() + DELAY_AFTER_ACCOUNT_OPEN;
    numberConsecutivePayments = 0;
    isOpen = true;
    balance = BigDecimal.valueOf(0);
  }

  public void addDeposit(BigDecimal amount) {
    balance = balance.add( amount);
    numberConsecutivePayments = 0;
    earliestNextActionTimestamp += DELAY_AFTER_TRANSACTION;
  }

  public void addPayment(BigDecimal amount) {
    balance = balance.subtract(amount);
    numberConsecutivePayments++;
    earliestNextActionTimestamp += DELAY_AFTER_TRANSACTION;
  }

  public void recordClosedPaymentAttempt() {
    numberPaymentAttemptsAfterClosed++;
  }
}

