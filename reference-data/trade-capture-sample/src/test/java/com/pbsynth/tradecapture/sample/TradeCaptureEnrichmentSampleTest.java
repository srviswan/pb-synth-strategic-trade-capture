package com.pbsynth.tradecapture.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pbsynth.referencedata.client.ReferenceDataClient;
import com.pbsynth.referencedata.client.SecurityDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TradeCaptureEnrichmentSampleTest {

  @Test
  void delegatesToReferenceDataClient() {
    ReferenceDataClient client = mock(ReferenceDataClient.class);
    when(client.getSecurityById(eq("SEC-001")))
        .thenReturn(
            Optional.of(
                new SecurityDto(
                    "SEC-001", "VOD.L", "GB00B16GWD43", "GBP", "EQUITY", "Vodafone", 1L)));
    when(client.batchGetSecurities(List.of("SEC-001"))).thenReturn(List.of());

    TradeCaptureEnrichmentSample sample = new TradeCaptureEnrichmentSample(client);
    assertThat(sample.resolveSecurity("SEC-001")).map(SecurityDto::ric).contains("VOD.L");
    sample.loadSecuritiesForAllocations(List.of("SEC-001"));
    verify(client).batchGetSecurities(List.of("SEC-001"));
  }
}
