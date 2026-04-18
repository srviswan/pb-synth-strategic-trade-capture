package com.pbsynth.referencedata.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ModuleDataSourcesConfiguration {

  @Bean
  @ConfigurationProperties(prefix = "refdata.datasource.security")
  public HikariDataSource securityDataSource() {
    return new HikariDataSource();
  }

  @Bean
  @ConfigurationProperties(prefix = "refdata.datasource.account")
  public HikariDataSource accountDataSource() {
    return new HikariDataSource();
  }

  @Bean
  @ConfigurationProperties(prefix = "refdata.datasource.book")
  public HikariDataSource bookDataSource() {
    return new HikariDataSource();
  }

  @Bean
  public JdbcTemplate securityJdbcTemplate(@Qualifier("securityDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  public JdbcTemplate accountJdbcTemplate(@Qualifier("accountDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }

  @Bean
  public JdbcTemplate bookJdbcTemplate(@Qualifier("bookDataSource") DataSource ds) {
    return new JdbcTemplate(ds);
  }
}
