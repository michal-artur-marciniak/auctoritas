package dev.auctoritas.auth.adapter.in.web;

public record InternalFacebookCallbackRequest(String code, String state, String callbackUri) {}
