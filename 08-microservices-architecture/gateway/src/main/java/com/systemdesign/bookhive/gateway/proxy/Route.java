package com.systemdesign.bookhive.gateway.proxy;

/** Gateway prefix -> owning service base URL. */
public record Route(String prefix, String targetBaseUrl) {
}
