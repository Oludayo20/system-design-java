package com.systemdesign.legacyinmemory.modules.identity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Row shape for the {@code users} table, owned exclusively by the identity module - no other
 * module is allowed to touch it directly, matching the original's comment in
 * {@code identity.module.js}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private int id;
    private String email;
    private String passwordHash;
    private List<String> roles;
}
