package com.pbsynth.referencedata.account.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AccountIngestionProperties.class)
public class AccountSyncConfiguration {

  @Bean(name = "accountSourceRestClient")
  @ConditionalOnProperty(prefix = "refdata.account.sync", name = "enabled", havingValue = "true")
  @ConditionalOnMissingBean(AccountSourceClient.class)
  public RestClient accountSourceRestClient(AccountIngestionProperties properties) {
    var source = properties.getSource();
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(source.getConnectTimeoutMs()));
    factory.setReadTimeout(Duration.ofMillis(source.getReadTimeoutMs()));
    return RestClient.builder()
        .requestFactory(factory)
        .baseUrl(source.getBaseUrl())
        .build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "refdata.account.sync", name = "enabled", havingValue = "true")
  @ConditionalOnMissingBean(AccountSourceClient.class)
  public AccountSourceClient accountSourceClient(
      @Qualifier("accountSourceRestClient") RestClient accountSourceRestClient,
      AccountIngestionProperties properties,
      ObjectMapper objectMapper) {
    return new RestAccountSourceClient(accountSourceRestClient, properties, objectMapper);
  }
}
