package com.systemdesign.ecommarketplace.users;

import com.systemdesign.ecommarketplace.common.CurrentUser;
import com.systemdesign.ecommarketplace.common.JwtPayload;
import com.systemdesign.ecommarketplace.users.dto.UpdateUserRequest;
import com.systemdesign.ecommarketplace.users.entity.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors src/modules/users/users.controller.ts. Protected - requires a valid bearer JWT. */
@Tag(name = "users")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users")
public class UsersController {

  private final UsersService usersService;

  public UsersController(UsersService usersService) {
    this.usersService = usersService;
  }

  @GetMapping("/me")
  public User me(@CurrentUser JwtPayload user) {
    // passwordHash is @JsonIgnore on the entity, so it never serializes -
    // same end result as the original's manual destructuring.
    return usersService.findById(user.sub());
  }

  @PatchMapping("/me")
  public User updateMe(@CurrentUser JwtPayload user, @Valid @RequestBody UpdateUserRequest dto) {
    return usersService.updateProfile(user.sub(), dto);
  }
}
