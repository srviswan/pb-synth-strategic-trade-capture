package com.pbsynth.referencedata.book;

public record BookRecord(
    String bookId,
    String code,
    String entityName,
    String desk,
    String normalisedKey,
    long version) {}
