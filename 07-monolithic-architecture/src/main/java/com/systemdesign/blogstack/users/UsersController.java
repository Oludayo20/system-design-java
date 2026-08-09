package com.systemdesign.blogstack.users;

import com.systemdesign.blogstack.auth.AuthenticatedUser;
import com.systemdesign.blogstack.shared.CurrentUser;
import com.systemdesign.blogstack.users.dto.UserResponse;
import com.systemdesign.blogstack.users.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "users", description = "User profile -- requires Bearer token")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users")
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user profile", description = "Resolves the JWT subject straight through UsersService.findById().")
    public UserResponse me(@CurrentUser AuthenticatedUser user) {
        User record = usersService.findById(user.userId());
        return new UserResponse(record.getId(), record.getEmail(), record.getDisplayName(), record.getCreatedAt());
    }
}
