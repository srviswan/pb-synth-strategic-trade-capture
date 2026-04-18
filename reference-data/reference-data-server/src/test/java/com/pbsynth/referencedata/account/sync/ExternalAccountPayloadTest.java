package com.pbsynth.referencedata.account.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbsynth.referencedata.account.AccountRecord;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalAccountPayloadTest {

  @Test
  void json_snakeCase_mapsToRecord() throws Exception {
    String json =
        """
        [{"account_id":"A1","name":"N","classification":"C","credit_tier":"T1","stp_eligible":false}]
        """;
    ObjectMapper mapper = new ObjectMapper();
    List<AccountRecord> rows = RestAccountSourceClient.parseJson(mapper, json);
    assertThat(rows).hasSize(1);
    AccountRecord r = rows.get(0);
    assertThat(r.accountId()).isEqualTo("A1");
    assertThat(r.name()).isEqualTo("N");
    assertThat(r.stpEligible()).isFalse();
  }
}
