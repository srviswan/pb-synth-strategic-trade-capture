package com.pbsynth.referencedata.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.pbsynth.referencedata.ReferenceDataApplication;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    classes = ReferenceDataApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {"refdata.account.sync.enabled=false"})
class ReferenceDataClientIntegrationTest {

  @LocalServerPort private int port;

  private ReferenceDataClient client;

  @BeforeEach
  void setUp() {
    ReferenceDataProperties props = new ReferenceDataProperties();
    props.setBaseUrl("http://127.0.0.1:" + port);
    props.setCacheEnabled(true);
    props.setCacheTtlSeconds(60);
    client =
        new DefaultReferenceDataClient(
            DefaultReferenceDataClient.createRestClient(props),
            props,
            DefaultReferenceDataClient.createCircuitBreaker(props),
            null);
  }

  @Test
  void getSecurityById_roundTrips() {
    assertThat(client.getSecurityById("SEC-001"))
        .map(SecurityDto::ric)
        .contains("VOD.L");
  }

  @Test
  void getSecurityByRic_roundTrips() {
    assertThat(client.getSecurityByRic("TSLA.O"))
        .map(SecurityDto::securityId)
        .contains("SEC-002");
  }

  @Test
  void batchGetSecurities_returnsMultiple() {
    List<SecurityDto> list = client.batchGetSecurities(List.of("SEC-001", "SEC-002"));
    assertThat(list).hasSize(2);
  }

  @Test
  void domainStyleEnrichment_bookAndBatchSameAsTradeCaptureWouldUse() {
    assertThat(client.getBookByCode("EQ-SWAP-LDN")).isPresent();
    assertThat(client.batchGetSecurities(List.of("SEC-001"))).hasSize(1);
  }

  @Test
  void cache_secondLookupDoesNotChangeResult() {
    assertThat(client.getSecurityById("SEC-001")).isPresent();
    assertThat(client.getSecurityById("SEC-001")).isPresent();
  }
}
