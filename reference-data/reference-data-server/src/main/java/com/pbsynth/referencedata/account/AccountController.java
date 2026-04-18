package com.pbsynth.referencedata.account;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

  private final AccountService service;

  public AccountController(AccountService service) {
    this.service = service;
  }

  @GetMapping("/{accountId}")
  public AccountResponse getById(@PathVariable("accountId") String accountId) {
    return AccountResponse.from(service.getById(accountId));
  }

  @PostMapping("/batch")
  public Map<String, Object> batch(@RequestBody BatchAccountRequest request) {
    List<AccountResponse> accounts =
        service.batchByIds(request.ids()).stream().map(AccountResponse::from).toList();
    return Map.of("accounts", accounts);
  }

  public record BatchAccountRequest(List<String> ids) {}

  public record AccountResponse(
      String accountId,
      String name,
      String classification,
      String creditTier,
      boolean stpEligible,
      long version) {

    static AccountResponse from(AccountRecord r) {
      return new AccountResponse(
          r.accountId(),
          r.name(),
          r.classification(),
          r.creditTier(),
          r.stpEligible(),
          r.version());
    }
  }
}
