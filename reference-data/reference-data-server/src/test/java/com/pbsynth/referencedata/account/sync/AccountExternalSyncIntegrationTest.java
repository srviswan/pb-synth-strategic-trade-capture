package com.pbsynth.referencedata.account.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pbsynth.referencedata.ReferenceDataApplication;
import com.pbsynth.referencedata.account.AccountRecord;
import com.pbsynth.referencedata.account.AccountRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(
    classes = ReferenceDataApplication.class,
    properties = {
      "refdata.account.sync.enabled=true",
      "refdata.account.source.base-url=http://127.0.0.1:9"
    })
class AccountExternalSyncIntegrationTest {

  @MockBean private AccountSourceClient accountSourceClient;

  @Autowired private AccountExternalSyncService syncService;

  @Autowired private AccountRepository accountRepository;

  @Test
  void syncNow_upsertsNewRowsAndIncrementsVersionForExisting() {
    when(accountSourceClient.fetchAccounts())
        .thenReturn(
            List.of(
                new AccountRecord(
                    "ACC-001", "Updated Name", "INSTITUTIONAL", "TIER_1", true, 99L),
                new AccountRecord("ACC-NEW", "New Acc", "RETAIL", "TIER_2", false, 99L)));

    syncService.syncNow();

    AccountRecord updated = accountRepository.findById("ACC-001").orElseThrow();
    assertThat(updated.name()).isEqualTo("Updated Name");
    assertThat(updated.version()).isEqualTo(2L);

    AccountRecord inserted = accountRepository.findById("ACC-NEW").orElseThrow();
    assertThat(inserted.name()).isEqualTo("New Acc");
    assertThat(inserted.stpEligible()).isFalse();
    assertThat(inserted.version()).isEqualTo(1L);
  }
}
