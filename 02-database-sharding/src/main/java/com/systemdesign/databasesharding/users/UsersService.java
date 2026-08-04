package com.systemdesign.databasesharding.users;

import com.systemdesign.databasesharding.common.IdGeneratorService;
import com.systemdesign.databasesharding.sharding.ShardManagerService;
import com.systemdesign.databasesharding.sharding.ShardResolutionContext;
import com.systemdesign.databasesharding.users.dto.CreateUserDto;
import com.systemdesign.databasesharding.users.dto.ShardCountDto;
import com.systemdesign.databasesharding.users.dto.ShardDistributionResponseDto;
import com.systemdesign.databasesharding.users.dto.UserResponseDto;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Mirrors users.service.ts. */
@Service
public class UsersService {

    private static final DateTimeFormatter ISO_INSTANT_MILLIS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final ShardManagerService shardManager;
    private final UsersRepository usersRepository;
    private final IdGeneratorService idGenerator;

    public UsersService(ShardManagerService shardManager, UsersRepository usersRepository,
                         IdGeneratorService idGenerator) {
        this.shardManager = shardManager;
        this.usersRepository = usersRepository;
        this.idGenerator = idGenerator;
    }

    public UserResponseDto create(CreateUserDto dto) {
        // Generate the global ID FIRST, then use it to pick a shard. This is
        // what lets IDs stay unique across shards - see IdGeneratorService.
        long id = idGenerator.nextId();
        ShardResolutionContext context = new ShardResolutionContext(dto.getRegion());
        int shardIndex = shardManager.resolveShardIndex(id, context);
        JdbcTemplate jdbcTemplate = shardManager.getTemplateForKey(id, context);

        try {
            UserRow row = usersRepository.insert(jdbcTemplate, id, dto.getEmail(), dto.getDisplayName(), dto.getRegion());
            return toResponseDto(row, shardIndex);
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email \"" + dto.getEmail() + "\" is already in use on shard " + shardIndex);
        }
    }

    public UserResponseDto findById(String id) {
        long numericId = parseId(id);
        int shardIndex = resolveShardIndexForLookup(numericId);
        JdbcTemplate jdbcTemplate = shardManager.getTemplateForKey(numericId);

        // Exactly one shard is queried here - never all of them.
        UserRow row = usersRepository.findById(jdbcTemplate, numericId);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User " + id + " not found on shard " + shardIndex);
        }
        return toResponseDto(row, shardIndex);
    }

    /**
     * The one legitimate cross-shard operation in this codebase: fan out a
     * COUNT(*) to every shard so a reader can see the distribution is
     * roughly even under the hash strategy. This is a debug/ops endpoint -
     * never call getAllTemplates() from a normal request path.
     */
    public ShardDistributionResponseDto getShardDistribution() {
        List<JdbcTemplate> templates = shardManager.getAllTemplates();
        List<ShardCountDto> shards = new ArrayList<>();
        long totalUsers = 0;

        for (int shardIndex = 0; shardIndex < templates.size(); shardIndex++) {
            long userCount = usersRepository.count(templates.get(shardIndex));
            shards.add(new ShardCountDto(shardIndex, userCount));
            totalUsers += userCount;
        }

        return new ShardDistributionResponseDto(shardManager.getStrategyName(), shards, totalUsers);
    }

    private int resolveShardIndexForLookup(long numericId) {
        try {
            return shardManager.resolveShardIndex(numericId);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot resolve a shard for id " + numericId + " without extra context under the \"" +
                            shardManager.getStrategyName() + "\" strategy. Geo sharding routes by region, so a " +
                            "lookup-by-id-alone endpoint would need a separate id -> shard directory in production " +
                            "(intentionally out of scope for this demo - see README).");
        }
    }

    private long parseId(String id) {
        try {
            long numericId = Long.parseLong(id);
            if (numericId < 0) {
                throw new NumberFormatException();
            }
            return numericId;
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "\"" + id + "\" is not a valid user id");
        }
    }

    private UserResponseDto toResponseDto(UserRow row, int shardIndex) {
        return new UserResponseDto(
                String.valueOf(row.id()),
                row.email(),
                row.displayName(),
                row.region(),
                ISO_INSTANT_MILLIS.format(row.createdAt()),
                shardIndex
        );
    }
}
