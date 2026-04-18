package com.pbsynth.tradecapture.sample;

import com.pbsynth.referencedata.client.AccountDto;
import com.pbsynth.referencedata.client.BookDto;
import com.pbsynth.referencedata.client.ReferenceDataClient;
import com.pbsynth.referencedata.client.SecurityDto;
import java.util.List;
import java.util.Optional;

/**
 * Example domain-side enrichment facade (same pattern as Trade Capture would use): inject
 * {@link ReferenceDataClient} only — no direct HTTP to reference data.
 */
public class TradeCaptureEnrichmentSample {

  private final ReferenceDataClient referenceData;

  public TradeCaptureEnrichmentSample(ReferenceDataClient referenceData) {
    this.referenceData = referenceData;
  }

  public Optional<SecurityDto> resolveSecurity(String securityId) {
    return referenceData.getSecurityById(securityId);
  }

  public Optional<SecurityDto> resolveSecurityByRic(String ric) {
    return referenceData.getSecurityByRic(ric);
  }

  public Optional<AccountDto> resolveAccount(String accountId) {
    return referenceData.getAccountById(accountId);
  }

  public Optional<BookDto> resolveBookByCode(String bookCode) {
    return referenceData.getBookByCode(bookCode);
  }

  /** Batch path for trade capture to avoid N+1 reference calls. */
  public List<SecurityDto> loadSecuritiesForAllocations(List<String> securityIds) {
    return referenceData.batchGetSecurities(securityIds);
  }
}
