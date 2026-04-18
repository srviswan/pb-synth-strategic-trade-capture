package com.pbsynth.referencedata.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BookDto(
    String bookId,
    String code,
    String entityName,
    String desk,
    String normalisedKey,
    long version) {}
