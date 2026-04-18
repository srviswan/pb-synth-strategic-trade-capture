package com.pbsynth.referencedata.client;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

public class DefaultReferenceDataClient implements ReferenceDataClient {

  private final RestClient rest;
  private final ReferenceDataProperties props;
  private final CircuitBreaker circuitBreaker;
  private final MeterRegistry meterRegistry;

  private final Cache<String, SecurityDto> securityHitCache;
  private final Cache<String, Boolean> securityMissCache;
  private final Cache<String, AccountDto> accountHitCache;
  private final Cache<String, Boolean> accountMissCache;
  private final Cache<String, BookDto> bookHitCache;
  private final Cache<String, Boolean> bookMissCache;

  public DefaultReferenceDataClient(
      RestClient rest,
      ReferenceDataProperties props,
      CircuitBreaker circuitBreaker,
      MeterRegistry meterRegistry) {
    this.rest = rest;
    this.props = props;
    this.circuitBreaker = circuitBreaker;
    this.meterRegistry = meterRegistry;

    this.securityHitCache = buildHitCache();
    this.securityMissCache = buildMissCache();
    this.accountHitCache = buildHitCache();
    this.accountMissCache = buildMissCache();
    this.bookHitCache = buildHitCache();
    this.bookMissCache = buildMissCache();
  }

  private <T> Cache<String, T> buildHitCache() {
    return Caffeine.newBuilder()
        .maximumSize(props.getMaxCacheEntries())
        .expireAfterWrite(Duration.ofSeconds(props.getCacheTtlSeconds()))
        .build();
  }

  private Cache<String, Boolean> buildMissCache() {
    return Caffeine.newBuilder()
        .maximumSize(props.getMaxCacheEntries())
        .expireAfterWrite(Duration.ofSeconds(props.getNegativeCacheTtlSeconds()))
        .build();
  }

  @Override
  public Optional<SecurityDto> getSecurityById(String securityId) {
    String key = "id:" + securityId;
    return cached(
        props.isCacheEnabled(),
        securityHitCache,
        securityMissCache,
        key,
        () -> getSecurityByIdRemote(securityId));
  }

  @Override
  public Optional<SecurityDto> getSecurityByRic(String ric) {
    String key = "ric:" + ric;
    return cached(
        props.isCacheEnabled(),
        securityHitCache,
        securityMissCache,
        key,
        () -> getSecurityByRicRemote(ric));
  }

  @Override
  public List<SecurityDto> batchGetSecurities(List<String> securityIds) {
    if (securityIds == null || securityIds.isEmpty()) {
      return List.of();
    }
    return circuitBreaker.executeSupplier(
        () -> {
          increment("refdata.client.calls", "operation", "batchSecurities");
          BatchIds body = new BatchIds(securityIds);
          SecuritiesPayload payload =
              rest
                  .post()
                  .uri("/api/v1/securities/batch")
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(body)
                  .retrieve()
                  .body(SecuritiesPayload.class);
          return payload != null && payload.securities() != null
              ? payload.securities()
              : List.of();
        });
  }

  @Override
  public Optional<AccountDto> getAccountById(String accountId) {
    String key = "id:" + accountId;
    return cached(
        props.isCacheEnabled(),
        accountHitCache,
        accountMissCache,
        key,
        () -> getAccountByIdRemote(accountId));
  }

  @Override
  public List<AccountDto> batchGetAccounts(List<String> accountIds) {
    if (accountIds == null || accountIds.isEmpty()) {
      return List.of();
    }
    return circuitBreaker.executeSupplier(
        () -> {
          increment("refdata.client.calls", "operation", "batchAccounts");
          AccountsPayload payload =
              rest
                  .post()
                  .uri("/api/v1/accounts/batch")
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(new BatchIds(accountIds))
                  .retrieve()
                  .body(AccountsPayload.class);
          return payload != null && payload.accounts() != null
              ? payload.accounts()
              : List.of();
        });
  }

  @Override
  public Optional<BookDto> getBookById(String bookId) {
    String key = "id:" + bookId;
    return cached(
        props.isCacheEnabled(),
        bookHitCache,
        bookMissCache,
        key,
        () -> getBookByIdRemote(bookId));
  }

  @Override
  public Optional<BookDto> getBookByCode(String code) {
    String key = "code:" + code;
    return cached(
        props.isCacheEnabled(),
        bookHitCache,
        bookMissCache,
        key,
        () -> getBookByCodeRemote(code));
  }

  @Override
  public List<BookDto> batchGetBooks(List<String> bookIds) {
    if (bookIds == null || bookIds.isEmpty()) {
      return List.of();
    }
    return circuitBreaker.executeSupplier(
        () -> {
          increment("refdata.client.calls", "operation", "batchBooks");
          BooksPayload payload =
              rest
                  .post()
                  .uri("/api/v1/books/batch")
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(new BatchIds(bookIds))
                  .retrieve()
                  .body(BooksPayload.class);
          return payload != null && payload.books() != null ? payload.books() : List.of();
        });
  }

  private <T> Optional<T> cached(
      boolean cacheEnabled,
      Cache<String, T> hitCache,
      Cache<String, Boolean> missCache,
      String key,
      Supplier<Optional<T>> remote) {
    if (!cacheEnabled) {
      return circuitBreaker.executeSupplier(remote::get);
    }
    T hit = hitCache.getIfPresent(key);
    if (hit != null) {
      increment("refdata.client.cache", "result", "hit");
      return Optional.of(hit);
    }
    if (missCache.getIfPresent(key) != null) {
      increment("refdata.client.cache", "result", "negative_hit");
      return Optional.empty();
    }
    Optional<T> result = circuitBreaker.executeSupplier(remote::get);
    result.ifPresent(v -> hitCache.put(key, v));
    if (result.isEmpty()) {
      missCache.put(key, Boolean.TRUE);
    }
    return result;
  }

  private Optional<SecurityDto> getSecurityByIdRemote(String id) {
    increment("refdata.client.calls", "operation", "getSecurityById");
    try {
      SecurityDto body =
          rest.get().uri("/api/v1/securities/{id}", id).retrieve().body(SecurityDto.class);
      return Optional.ofNullable(body);
    } catch (HttpClientErrorException e) {
      if (isNotFound(e.getStatusCode())) {
        return Optional.empty();
      }
      throw e;
    }
  }

  private Optional<SecurityDto> getSecurityByRicRemote(String ric) {
    increment("refdata.client.calls", "operation", "getSecurityByRic");
    try {
      SecurityDto body =
          rest
              .get()
              .uri("/api/v1/securities/by-ric?ric={ric}", ric)
              .retrieve()
              .body(SecurityDto.class);
      return Optional.ofNullable(body);
    } catch (HttpClientErrorException e) {
      if (isNotFound(e.getStatusCode())) {
        return Optional.empty();
      }
      throw e;
    }
  }

  private Optional<AccountDto> getAccountByIdRemote(String id) {
    increment("refdata.client.calls", "operation", "getAccountById");
    try {
      AccountDto body =
          rest.get().uri("/api/v1/accounts/{id}", id).retrieve().body(AccountDto.class);
      return Optional.ofNullable(body);
    } catch (HttpClientErrorException e) {
      if (isNotFound(e.getStatusCode())) {
        return Optional.empty();
      }
      throw e;
    }
  }

  private Optional<BookDto> getBookByIdRemote(String id) {
    increment("refdata.client.calls", "operation", "getBookById");
    try {
      BookDto body = rest.get().uri("/api/v1/books/{id}", id).retrieve().body(BookDto.class);
      return Optional.ofNullable(body);
    } catch (HttpClientErrorException e) {
      if (isNotFound(e.getStatusCode())) {
        return Optional.empty();
      }
      throw e;
    }
  }

  private Optional<BookDto> getBookByCodeRemote(String code) {
    increment("refdata.client.calls", "operation", "getBookByCode");
    try {
      BookDto body =
          rest
              .get()
              .uri("/api/v1/books/by-code?code={code}", code)
              .retrieve()
              .body(BookDto.class);
      return Optional.ofNullable(body);
    } catch (HttpClientErrorException e) {
      if (isNotFound(e.getStatusCode())) {
        return Optional.empty();
      }
      throw e;
    }
  }

  private static boolean isNotFound(HttpStatusCode status) {
    return status.value() == 404;
  }

  private void increment(String name, String tagKey, String tagValue) {
    if (meterRegistry != null) {
      Counter.builder(name).tag(tagKey, tagValue).register(meterRegistry).increment();
    }
  }

  private record BatchIds(List<String> ids) {}

  private record SecuritiesPayload(List<SecurityDto> securities) {}

  private record AccountsPayload(List<AccountDto> accounts) {}

  private record BooksPayload(List<BookDto> books) {}

  public static CircuitBreaker createCircuitBreaker(ReferenceDataProperties props) {
    ReferenceDataProperties.CircuitBreakerProps cbp = props.getCircuitBreaker();
    CircuitBreakerConfig config =
        CircuitBreakerConfig.custom()
            .failureRateThreshold(cbp.getFailureRateThreshold())
            .waitDurationInOpenState(Duration.ofSeconds(cbp.getWaitOpenSeconds()))
            .slidingWindowSize(cbp.getSlidingWindowSize())
            .minimumNumberOfCalls(cbp.getMinimumNumberOfCalls())
            .build();
    return CircuitBreaker.of("reference-data", config);
  }

  public static RestClient createRestClient(ReferenceDataProperties props) {
    return RestClient.builder().baseUrl(props.getBaseUrl()).build();
  }
}
