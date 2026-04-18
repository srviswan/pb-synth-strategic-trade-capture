package com.pbsynth.referencedata.security;

public record SecurityRecord(
    String securityId,
    String ric,
    String isin,
    String currency,
    String assetType,
    String description,
    long version) {}
