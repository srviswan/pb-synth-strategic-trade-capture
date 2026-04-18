package com.pbsynth.referencedata.client;

import java.util.List;
import java.util.Optional;

/**
 * Facade for Security, Account, and Book reference data. Domain services (Trade Capture, Position, etc.)
 * depend on this interface only.
 */
public interface ReferenceDataClient {

  Optional<SecurityDto> getSecurityById(String securityId);

  Optional<SecurityDto> getSecurityByRic(String ric);

  List<SecurityDto> batchGetSecurities(List<String> securityIds);

  Optional<AccountDto> getAccountById(String accountId);

  List<AccountDto> batchGetAccounts(List<String> accountIds);

  Optional<BookDto> getBookById(String bookId);

  Optional<BookDto> getBookByCode(String code);

  List<BookDto> batchGetBooks(List<String> bookIds);
}
