package com.systemdesign.databasesharding.users;

import com.systemdesign.databasesharding.users.dto.CreateUserDto;
import com.systemdesign.databasesharding.users.dto.ShardDistributionResponseDto;
import com.systemdesign.databasesharding.users.dto.UserResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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

@Tag(name = "users", description = "Sharded user CRUD and debug distribution")
@RestController
@RequestMapping("/users")
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a user",
            description = "Generates a global ID first, then routes the record to exactly one shard using SHARDING_STRATEGY.")
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    public UserResponseDto create(@Valid @RequestBody CreateUserDto dto) {
        return usersService.create(dto);
    }

    @GetMapping("/_debug/distribution")
    @Operation(
            summary = "DEBUG: per-shard row counts",
            description = "The only endpoint allowed to query every shard. Never use on the production hot path.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ShardDistributionResponseDto.class)))
    public ShardDistributionResponseDto getDistribution() {
        return usersService.getShardDistribution();
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Fetch a user by ID",
            description = "Computes shard(id) and queries only that shard. Geo strategy returns 400 — cannot resolve shard from id alone.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = UserResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "User not found on its resolved shard.")
    @ApiResponse(responseCode = "400", description = "Geo strategy: cannot resolve shard from id alone.")
    public UserResponseDto findById(
            @Parameter(description = "Numeric user ID", example = "1927841923837952") @PathVariable String id) {
        return usersService.findById(id);
    }
}
