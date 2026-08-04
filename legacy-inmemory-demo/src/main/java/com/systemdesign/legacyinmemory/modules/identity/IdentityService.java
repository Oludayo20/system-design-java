package com.systemdesign.legacyinmemory.modules.identity;

import com.systemdesign.legacyinmemory.common.AppException;
import com.systemdesign.legacyinmemory.infrastructure.InMemoryDatabase;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * Java port of {@code modules/identity/identity.module.js} - "Login, Registration, JWT, Roles,
 * Permissions." Owns the {@code users} table.
 */
@Service
public class IdentityService {

    private final InMemoryDatabase db;
    private final AtomicInteger nextUserId = new AtomicInteger(1);

    public IdentityService(InMemoryDatabase db) {
        this.db = db;
    }

    public UserView register(String email, String password) {
        User user = new User(nextUserId.getAndIncrement(), email, hash(password), List.of("customer"));
        db.insert("users", user);
        System.out.println("[Identity] registered user #" + user.getId() + " (" + email + ")");
        return new UserView(user.getId(), user.getEmail(), user.getRoles());
    }

    public LoginResult login(String email, String password) {
        Optional<User> found = db.find("users", (User u) -> u.getEmail().equals(email));
        if (found.isEmpty() || !found.get().getPasswordHash().equals(hash(password))) {
            throw new AppException(401, "Invalid credentials");
        }
        User user = found.get();
        // Stand-in for a signed JWT, matching the original's
        // `jwt.${Buffer.from('user:' + id).toString('base64url')}.signature`.
        String token = "jwt." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("user:" + user.getId()).getBytes(StandardCharsets.UTF_8)) + ".signature";
        System.out.println("[Identity] issued token for user #" + user.getId());
        return new LoginResult(token, new UserView(user.getId(), user.getEmail(), user.getRoles()));
    }

    private static String hash(String password) {
        return "hashed(" + password + ")";
    }
}
