package com.systemdesign.bookhive.catalog.shared.exception;

public record ApiError(int statusCode, Object message, String error) {
}
