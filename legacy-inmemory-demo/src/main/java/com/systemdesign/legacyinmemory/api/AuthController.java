package com.systemdesign.legacyinmemory.api;

import com.systemdesign.legacyinmemory.api.dto.LoginRequest;
import com.systemdesign.legacyinmemory.api.dto.RegisterRequest;
import com.systemdesign.legacyinmemory.modules.identity.IdentityService;
import com.systemdesign.legacyinmemory.modules.identity.LoginResult;
import com.systemdesign.legacyinmemory.modules.identity.UserView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Java port of the {@code /auth/*} routes in {@code api/server.js}. This controller has no
 * business logic of its own - it just routes to {@link IdentityService}, exactly like the
 * original's comment: "The API decides which module should handle the request."
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final IdentityService identityService;

    public AuthController(IdentityService identityService) {
        this.identityService = identityService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserView> register(@RequestBody RegisterRequest request) {
        UserView user = identityService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResult> login(@RequestBody LoginRequest request) {
        LoginResult result = identityService.login(request.email(), request.password());
        return ResponseEntity.ok(result);
    }
}
