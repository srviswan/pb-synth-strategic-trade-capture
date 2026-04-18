package com.pbsynth.referencedata.account.sync;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pbsynth.referencedata.account.AccountRecord;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalAccountPayload(
    @JsonAlias({"accountId", "account_id"}) String accountId,
    String name,
    String classification,
    @JsonAlias({"creditTier", "credit_tier"}) String creditTier,
    @JsonAlias({"stpEligible", "stp_eligible"}) Boolean stpEligible) {

  public AccountRecord toRecord() {
    boolean stp = stpEligible != null && stpEligible;
    return new AccountRecord(
        accountId, name, classification, creditTier, stp, 1L);
  }
}
