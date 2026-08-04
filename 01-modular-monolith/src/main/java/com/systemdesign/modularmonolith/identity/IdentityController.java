package com.systemdesign.modularmonolith.identity;

import com.systemdesign.modularmonolith.identity.dto.AuthResponse;
import com.systemdesign.modularmonolith.identity.dto.LoginRequest;
import com.systemdesign.modularmonolith.identity.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors {@code src/modules/identity/identity.controller.ts}. */
@RestController
@RequestMapping("/auth")
public class IdentityController {

    private final IdentityService identityService;

    public IdentityController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest dto) {
        return identityService.register(dto);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody LoginRequest dto) {
        return identityService.login(dto);
    }
}
