package com.systemdesign.databasesharding.users;

import com.systemdesign.databasesharding.users.dto.CreateUserDto;
import com.systemdesign.databasesharding.users.dto.ShardDistributionResponseDto;
import com.systemdesign.databasesharding.users.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors users.controller.ts. */
@Tag(name = "users")
@RestController
@RequestMapping("/users")
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user. Generates a global id, then routes to one shard.")
    @ApiResponse(responseCode = "201")
    public UserResponseDto create(@Valid @RequestBody CreateUserDto dto) {
        return usersService.create(dto);
    }

    @GetMapping("/_debug/distribution")
    @Operation(
            summary = "DEBUG/OPS ONLY: COUNT(*) on every shard to visualize load distribution.",
            description = "The one endpoint in this service allowed to query every shard. Never used on the hot path."
    )
    @ApiResponse(responseCode = "200")
    public ShardDistributionResponseDto getDistribution() {
        return usersService.getShardDistribution();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a user. Resolves the owning shard and queries only that one.")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "404", description = "User not found on its resolved shard.")
    public UserResponseDto findById(@PathVariable String id) {
        return usersService.findById(id);
    }
}
