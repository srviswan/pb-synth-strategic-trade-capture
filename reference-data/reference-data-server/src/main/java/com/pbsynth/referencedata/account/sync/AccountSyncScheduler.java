package com.pbsynth.referencedata.account.sync;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "refdata.account.sync", name = "enabled", havingValue = "true")
public class AccountSyncScheduler {

  private final AccountExternalSyncService syncService;

  public AccountSyncScheduler(AccountExternalSyncService syncService) {
    this.syncService = syncService;
  }

  @Scheduled(cron = "${refdata.account.sync.cron:0 0 * * * *}")
  public void runScheduledSync() {
    syncService.syncNow();
  }
}
