package org.sudhir512kj.webclient.model;

public record Post(
    Long id,
    Long userId,
    String title,
    String body
) {}