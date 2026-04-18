package com.pbsynth.referencedata.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "reference-data.client")
public class ReferenceDataProperties {

  /** Base URL of Reference Data Service, e.g. http://localhost:8090 */
  private String baseUrl = "http://localhost:8090";

  private boolean enabled = true;

  /** Enable Caffeine cache for GET operations */
  private boolean cacheEnabled = true;

  /** TTL for successful entity cache entries */
  private long cacheTtlSeconds = 300;

  /** TTL for negative (404) cache entries */
  private long negativeCacheTtlSeconds = 60;

  private int maxCacheEntries = 10_000;

  private CircuitBreakerProps circuitBreaker = new CircuitBreakerProps();

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isCacheEnabled() {
    return cacheEnabled;
  }

  public void setCacheEnabled(boolean cacheEnabled) {
    this.cacheEnabled = cacheEnabled;
  }

  public long getCacheTtlSeconds() {
    return cacheTtlSeconds;
  }

  public void setCacheTtlSeconds(long cacheTtlSeconds) {
    this.cacheTtlSeconds = cacheTtlSeconds;
  }

  public long getNegativeCacheTtlSeconds() {
    return negativeCacheTtlSeconds;
  }

  public void setNegativeCacheTtlSeconds(long negativeCacheTtlSeconds) {
    this.negativeCacheTtlSeconds = negativeCacheTtlSeconds;
  }

  public int getMaxCacheEntries() {
    return maxCacheEntries;
  }

  public void setMaxCacheEntries(int maxCacheEntries) {
    this.maxCacheEntries = maxCacheEntries;
  }

  public CircuitBreakerProps getCircuitBreaker() {
    return circuitBreaker;
  }

  public void setCircuitBreaker(CircuitBreakerProps circuitBreaker) {
    this.circuitBreaker = circuitBreaker;
  }

  public static class CircuitBreakerProps {
    private int failureRateThreshold = 50;
    private int waitOpenSeconds = 30;
    private int slidingWindowSize = 20;
    private int minimumNumberOfCalls = 10;

    public int getFailureRateThreshold() {
      return failureRateThreshold;
    }

    public void setFailureRateThreshold(int failureRateThreshold) {
      this.failureRateThreshold = failureRateThreshold;
    }

    public int getWaitOpenSeconds() {
      return waitOpenSeconds;
    }

    public void setWaitOpenSeconds(int waitOpenSeconds) {
      this.waitOpenSeconds = waitOpenSeconds;
    }

    public int getSlidingWindowSize() {
      return slidingWindowSize;
    }

    public void setSlidingWindowSize(int slidingWindowSize) {
      this.slidingWindowSize = slidingWindowSize;
    }

    public int getMinimumNumberOfCalls() {
      return minimumNumberOfCalls;
    }

    public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
      this.minimumNumberOfCalls = minimumNumberOfCalls;
    }
  }
}
