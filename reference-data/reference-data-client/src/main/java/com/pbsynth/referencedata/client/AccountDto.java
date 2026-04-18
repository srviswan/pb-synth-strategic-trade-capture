package com.pbsynth.referencedata.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountDto(
    String accountId,
    String name,
    String classification,
    String creditTier,
    boolean stpEligible,
    long version) {}
