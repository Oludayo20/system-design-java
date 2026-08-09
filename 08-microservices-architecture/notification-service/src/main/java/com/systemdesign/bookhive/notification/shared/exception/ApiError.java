package com.systemdesign.bookhive.notification.shared.exception;

public record ApiError(int statusCode, Object message, String error) {
}
