package com.pbsynth.referencedata.account.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "refdata.account")
public class AccountIngestionProperties {

  private final Sync sync = new Sync();
  private final Source source = new Source();

  public Sync getSync() {
    return sync;
  }

  public Source getSource() {
    return source;
  }

  public static class Sync {
    /** When true, hourly (or cron) ingestion runs and REST client beans are created. */
    private boolean enabled = false;

    /** Spring six-field cron (second minute hour day month weekday). Default: top of every hour. */
    private String cron = "0 0 * * * *";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getCron() {
      return cron;
    }

    public void setCron(String cron) {
      this.cron = cron;
    }
  }

  public static class Source {
    private String baseUrl = "http://127.0.0.1:9999";
    /** Path appended to base URL (e.g. /accounts). */
    private String path = "/accounts";
    private int connectTimeoutMs = 5_000;
    private int readTimeoutMs = 15_000;

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getPath() {
      return path;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public int getConnectTimeoutMs() {
      return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
      this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
      return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
      this.readTimeoutMs = readTimeoutMs;
    }
  }
}
