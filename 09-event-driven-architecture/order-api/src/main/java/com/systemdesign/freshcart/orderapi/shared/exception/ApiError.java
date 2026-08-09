package com.systemdesign.freshcart.orderapi.shared.exception;

public record ApiError(int statusCode, Object message, String error) {
}
