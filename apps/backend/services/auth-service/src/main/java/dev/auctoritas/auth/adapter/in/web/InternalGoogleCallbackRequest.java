package dev.auctoritas.auth.adapter.in.web;

public record InternalGoogleCallbackRequest(String code, String state, String callbackUri) {}
