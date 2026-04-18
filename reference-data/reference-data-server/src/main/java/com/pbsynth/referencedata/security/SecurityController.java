package com.pbsynth.referencedata.security;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/securities")
public class SecurityController {

  private final SecurityService service;

  public SecurityController(SecurityService service) {
    this.service = service;
  }

  @GetMapping("/{securityId}")
  public SecurityResponse getById(@PathVariable("securityId") String securityId) {
    return SecurityResponse.from(service.getById(securityId));
  }

  @GetMapping("/by-ric")
  public SecurityResponse getByRic(@RequestParam("ric") String ric) {
    return SecurityResponse.from(service.getByRic(ric));
  }

  @PostMapping("/batch")
  public Map<String, Object> batch(@RequestBody BatchSecurityRequest request) {
    List<SecurityResponse> securities =
        service.batchByIds(request.ids()).stream().map(SecurityResponse::from).toList();
    return Map.of("securities", securities);
  }

  public record BatchSecurityRequest(List<String> ids) {}

  public record SecurityResponse(
      String securityId,
      String ric,
      String isin,
      String currency,
      String assetType,
      String description,
      long version) {

    static SecurityResponse from(SecurityRecord r) {
      return new SecurityResponse(
          r.securityId(), r.ric(), r.isin(), r.currency(), r.assetType(), r.description(), r.version());
    }
  }
}
