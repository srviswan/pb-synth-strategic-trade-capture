package com.pbsynth.referencedata.account.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbsynth.referencedata.account.AccountRecord;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

public class RestAccountSourceClient implements AccountSourceClient {

  private final RestClient restClient;
  private final AccountIngestionProperties properties;
  private final ObjectMapper objectMapper;

  public RestAccountSourceClient(
      RestClient accountSourceRestClient,
      AccountIngestionProperties properties,
      ObjectMapper objectMapper) {
    this.restClient = accountSourceRestClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<AccountRecord> fetchAccounts() {
    String path = properties.getSource().getPath();
    byte[] raw =
        restClient
            .get()
            .uri(path)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(byte[].class);
    if (raw == null || raw.length == 0) {
      return List.of();
    }
    return parsePayload(raw);
  }

  private List<AccountRecord> parsePayload(byte[] raw) {
    try {
      ExternalAccountPayload[] array =
          objectMapper.readValue(raw, ExternalAccountPayload[].class);
      return Arrays.stream(array).map(ExternalAccountPayload::toRecord).toList();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse external account JSON array", e);
    }
  }

  /** For tests: same parsing rules as HTTP body. */
  public static List<AccountRecord> parseJson(ObjectMapper mapper, String json)
      throws IOException {
    ExternalAccountPayload[] array = mapper.readValue(json, ExternalAccountPayload[].class);
    return Arrays.stream(array).map(ExternalAccountPayload::toRecord).toList();
  }
}
