package com.systemdesign.ecommarketplace.users;

import com.systemdesign.ecommarketplace.common.exceptions.NotFoundException;
import com.systemdesign.ecommarketplace.sharding.ShardRouterService;
import com.systemdesign.ecommarketplace.users.dto.UpdateUserRequest;
import com.systemdesign.ecommarketplace.users.entity.User;
import com.systemdesign.ecommarketplace.users.repository.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mirrors src/modules/users/users.service.ts. Every method resolves the
 * shard from userId FIRST and then issues a single query against that one
 * shard - never a scatter-gather across all three. That's the whole point
 * of sharding by userId: a request that already knows the userId (which
 * every authenticated request does, via the JWT `sub` claim) never needs to
 * ask more than one database.
 */
@Service
public class UsersService {

  private static final Logger log = LoggerFactory.getLogger(UsersService.class);

  private final ShardRouterService shardRouter;

  public UsersService(ShardRouterService shardRouter) {
    this.shardRouter = shardRouter;
  }

  public User findById(String userId) {
    int shardIndex = shardRouter.resolveShardIndex(userId);
    log.debug("Resolving user {} -> shard {}", userId, shardIndex);
    UserRepository repo = shardRouter.getRepository(UserRepository.class, shardIndex);
    return repo.findById(UUID.fromString(userId))
        .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
  }

  public User updateProfile(String userId, UpdateUserRequest dto) {
    int shardIndex = shardRouter.resolveShardIndex(userId);
    TransactionTemplate tx = shardRouter.getTransactionTemplate(shardIndex);

    return tx.execute(
        status -> {
          UserRepository repo = shardRouter.getRepository(UserRepository.class, shardIndex);
          User user =
              repo.findById(UUID.fromString(userId))
                  .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
          if (dto.fullName() != null) {
            user.setFullName(dto.fullName());
          }
          return repo.save(user);
        });
  }
}
