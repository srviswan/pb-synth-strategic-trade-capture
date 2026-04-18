package com.pbsynth.referencedata.account.sync;

import com.pbsynth.referencedata.account.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "refdata.account.sync", name = "enabled", havingValue = "true")
public class AccountExternalSyncService {

  private static final Logger log = LoggerFactory.getLogger(AccountExternalSyncService.class);

  private final AccountSourceClient accountSourceClient;
  private final AccountRepository accountRepository;

  public AccountExternalSyncService(
      AccountSourceClient accountSourceClient, AccountRepository accountRepository) {
    this.accountSourceClient = accountSourceClient;
    this.accountRepository = accountRepository;
  }

  /** Invoked by the scheduler and available for tests / manual ops. */
  public void syncNow() {
    try {
      var records = accountSourceClient.fetchAccounts();
      accountRepository.upsertBatch(records);
      log.info("Account sync finished: {} row(s) upserted", records.size());
    } catch (RuntimeException ex) {
      log.error("Account sync failed", ex);
      throw ex;
    }
  }
}
