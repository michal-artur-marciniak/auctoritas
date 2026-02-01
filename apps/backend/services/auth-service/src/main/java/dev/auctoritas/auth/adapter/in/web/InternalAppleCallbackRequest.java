package dev.auctoritas.auth.adapter.in.web;

public record InternalAppleCallbackRequest(String code, String state, String callbackUri) {}
