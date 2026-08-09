package com.systemdesign.blogstack.users;

import com.systemdesign.blogstack.shared.exception.NotFoundException;
import com.systemdesign.blogstack.users.entity.User;
import com.systemdesign.blogstack.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UsersService {

    private final UserRepository users;

    public UsersService(UserRepository users) {
        this.users = users;
    }

    public Optional<User> findByEmail(String email) {
        return users.findByEmail(email);
    }

    /**
     * Plain in-process lookup. In this monolith, any module that injects {@link UsersService} can
     * call this directly -- there is no interface/HTTP boundary enforcing who's allowed to ask.
     * {@link com.systemdesign.blogstack.comments.CommentsService} calls it straight, just like
     * {@link com.systemdesign.blogstack.auth.AuthService} does.
     */
    public User findById(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    public User create(String email, String passwordHash, String displayName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setDisplayName(displayName);
        return users.save(user);
    }
}
