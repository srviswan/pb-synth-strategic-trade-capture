package com.pbsynth.referencedata.account;

import com.pbsynth.referencedata.api.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

  private final AccountRepository repository;

  public AccountService(AccountRepository repository) {
    this.repository = repository;
  }

  public AccountRecord getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "ACCOUNT_NOT_FOUND", "Account not found for id: " + id));
  }

  public List<AccountRecord> batchByIds(List<String> ids) {
    return repository.findAllByIdIn(ids);
  }
}
