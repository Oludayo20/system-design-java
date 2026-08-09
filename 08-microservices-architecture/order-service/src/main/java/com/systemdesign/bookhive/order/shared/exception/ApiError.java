package com.systemdesign.bookhive.order.shared.exception;

public record ApiError(int statusCode, Object message, String error) {
}
