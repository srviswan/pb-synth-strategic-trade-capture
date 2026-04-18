package com.pbsynth.referencedata.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(ReferenceDataProperties.class)
@ConditionalOnProperty(
    prefix = "reference-data.client",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ReferenceDataClientAutoConfiguration {

  public static final String REST_CLIENT_BEAN = "referenceDataRestClient";
  public static final String CIRCUIT_BREAKER_BEAN = "referenceDataCircuitBreaker";

  @Bean(REST_CLIENT_BEAN)
  @ConditionalOnMissingBean(name = REST_CLIENT_BEAN)
  public RestClient referenceDataRestClient(ReferenceDataProperties props) {
    return DefaultReferenceDataClient.createRestClient(props);
  }

  @Bean(CIRCUIT_BREAKER_BEAN)
  @ConditionalOnMissingBean(name = CIRCUIT_BREAKER_BEAN)
  public CircuitBreaker referenceDataCircuitBreaker(ReferenceDataProperties props) {
    return DefaultReferenceDataClient.createCircuitBreaker(props);
  }

  @Bean
  @ConditionalOnMissingBean
  public ReferenceDataClient referenceDataClient(
      @Qualifier(REST_CLIENT_BEAN) RestClient referenceDataRestClient,
      ReferenceDataProperties props,
      @Qualifier(CIRCUIT_BREAKER_BEAN) CircuitBreaker referenceDataCircuitBreaker,
      ObjectProvider<MeterRegistry> meterRegistry) {
    return new DefaultReferenceDataClient(
        referenceDataRestClient,
        props,
        referenceDataCircuitBreaker,
        meterRegistry.getIfAvailable());
  }
}
