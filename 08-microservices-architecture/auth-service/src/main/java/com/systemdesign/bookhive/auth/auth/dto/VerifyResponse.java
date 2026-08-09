package com.systemdesign.bookhive.auth.auth.dto;

import com.systemdesign.bookhive.auth.common.JwtPayload;

public record VerifyResponse(boolean valid, JwtPayload payload) {
}
