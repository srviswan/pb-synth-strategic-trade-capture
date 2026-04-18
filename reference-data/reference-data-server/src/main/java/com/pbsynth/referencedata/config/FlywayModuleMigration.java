package com.pbsynth.referencedata.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayModuleMigration {

  @Bean
  public Object runSecurityMigrations(@Qualifier("securityDataSource") DataSource ds) {
    Flyway.configure().dataSource(ds).locations("classpath:db/security").load().migrate();
    return new Object();
  }

  @Bean
  public Object runAccountMigrations(@Qualifier("accountDataSource") DataSource ds) {
    Flyway.configure().dataSource(ds).locations("classpath:db/account").load().migrate();
    return new Object();
  }

  @Bean
  public Object runBookMigrations(@Qualifier("bookDataSource") DataSource ds) {
    Flyway.configure().dataSource(ds).locations("classpath:db/book").load().migrate();
    return new Object();
  }
}
