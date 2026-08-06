package com.systemdesign.ecommarketplace.users;

import com.systemdesign.ecommarketplace.common.CurrentUser;
import com.systemdesign.ecommarketplace.common.JwtPayload;
import com.systemdesign.ecommarketplace.users.dto.UpdateUserRequest;
import com.systemdesign.ecommarketplace.users.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "users", description = "Sharded user profile — requires JWT")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users")
public class UsersController {

  private final UsersService usersService;

  public UsersController(UsersService usersService) {
    this.usersService = usersService;
  }

  @GetMapping("/me")
  @Operation(summary = "Get current user profile", description = "Reads from the shard resolved by hash(userId) % 3.")
  @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = User.class)))
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
  public User me(@CurrentUser JwtPayload user) {
    return usersService.findById(user.sub());
  }

  @PatchMapping("/me")
  @Operation(summary = "Update current user profile")
  @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = User.class)))
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
  public User updateMe(@CurrentUser JwtPayload user, @Valid @RequestBody UpdateUserRequest dto) {
    return usersService.updateProfile(user.sub(), dto);
  }
}
