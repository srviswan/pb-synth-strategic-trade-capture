package com.pbsynth.referencedata.security;

import com.pbsynth.referencedata.api.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

  private final SecurityRepository repository;

  public SecurityService(SecurityRepository repository) {
    this.repository = repository;
  }

  public SecurityRecord getById(String id) {
    return repository
        .findById(id)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "SECURITY_NOT_FOUND", "Security not found for id: " + id));
  }

  public SecurityRecord getByRic(String ric) {
    return repository
        .findByRic(ric)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "SECURITY_NOT_FOUND", "Security not found for RIC: " + ric));
  }

  public List<SecurityRecord> batchByIds(List<String> ids) {
    return repository.findAllByIdIn(ids);
  }
}
