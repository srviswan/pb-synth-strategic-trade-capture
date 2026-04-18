package com.pbsynth.referencedata.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SecurityDto(
    String securityId,
    String ric,
    String isin,
    String currency,
    String assetType,
    String description,
    long version) {}
