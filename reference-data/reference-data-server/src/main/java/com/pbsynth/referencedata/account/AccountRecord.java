package com.pbsynth.referencedata.account;

public record AccountRecord(
    String accountId,
    String name,
    String classification,
    String creditTier,
    boolean stpEligible,
    long version) {}
