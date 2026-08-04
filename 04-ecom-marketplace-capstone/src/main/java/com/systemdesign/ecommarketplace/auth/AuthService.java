package com.systemdesign.ecommarketplace.auth;

import com.systemdesign.ecommarketplace.auth.dto.AuthResponse;
import com.systemdesign.ecommarketplace.auth.dto.AuthUserSummary;
import com.systemdesign.ecommarketplace.auth.dto.LoginRequest;
import com.systemdesign.ecommarketplace.auth.dto.RegisterRequest;
import com.systemdesign.ecommarketplace.auth.entity.UserDirectory;
import com.systemdesign.ecommarketplace.auth.repository.UserDirectoryRepository;
import com.systemdesign.ecommarketplace.common.JwtPayload;
import com.systemdesign.ecommarketplace.common.JwtService;
import com.systemdesign.ecommarketplace.common.exceptions.ConflictException;
import com.systemdesign.ecommarketplace.common.exceptions.UnauthorizedException;
import com.systemdesign.ecommarketplace.sharding.ShardRouterService;
import com.systemdesign.ecommarketplace.users.entity.User;
import com.systemdesign.ecommarketplace.users.repository.UserRepository;
import com.systemdesign.ecommarketplace.wallet.WalletService;
import com.systemdesign.ecommarketplace.wallet.entity.Wallet;
import com.systemdesign.ecommarketplace.wallet.repository.WalletRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Mirrors src/modules/auth/auth.service.ts. */
@Service
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserDirectoryRepository directoryRepository;
  private final ShardRouterService shardRouter;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      UserDirectoryRepository directoryRepository,
      ShardRouterService shardRouter,
      JwtService jwtService,
      PasswordEncoder passwordEncoder) {
    this.directoryRepository = directoryRepository;
    this.shardRouter = shardRouter;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Registration order matters:
   *
   * <ol>
   *   <li>Generate userId (the shard key itself - nothing to shard-resolve before this exists).
   *   <li>Resolve the shard from that id.
   *   <li>Write User + Wallet atomically on that ONE shard.
   *   <li>Only then write the primary-DB email-&gt;shard directory entry.
   *   <li>Only then issue the JWT.
   * </ol>
   *
   * <p>Step 4 is a second, independent database write with no distributed
   * transaction tying it to step 3 (Postgres doesn't support 2PC across two
   * separate connections/instances out of the box, and adding an external
   * coordinator is overkill for this demo). If step 4 fails, we compensate
   * by deleting the shard row rather than leaving an orphaned account
   * nobody can log into. A crash between step 3 and step 4 completing (e.g.
   * the process dying) is the one gap this doesn't close - production would
   * use a transactional outbox on the shard + a reconciliation job instead
   * of a synchronous compensating delete.
   */
  public AuthResponse register(RegisterRequest dto) {
    if (directoryRepository.findByEmail(dto.email()).isPresent()) {
      throw new ConflictException("Email already registered");
    }

    UUID userId = UUID.randomUUID();
    int shardIndex = shardRouter.resolveShardIndex(userId.toString());
    String passwordHash = passwordEncoder.encode(dto.password());

    log.info("Registering {} -> userId={} -> shard={}", dto.email(), userId, shardIndex);

    shardRouter
        .getTransactionTemplate(shardIndex)
        .execute(
            status -> {
              UserRepository userRepo = shardRouter.getRepository(UserRepository.class, shardIndex);
              WalletRepository walletRepo = shardRouter.getRepository(WalletRepository.class, shardIndex);

              User user = new User();
              user.setId(userId);
              user.setEmail(dto.email());
              user.setPasswordHash(passwordHash);
              user.setFullName(dto.fullName());
              userRepo.save(user);

              Wallet wallet = new Wallet();
              wallet.setId(UUID.randomUUID());
              wallet.setUserId(userId);
              wallet.setBalanceCents(WalletService.SIGNUP_BONUS_CENTS);
              walletRepo.save(wallet);
              return null;
            });

    try {
      UserDirectory entry = new UserDirectory();
      entry.setId(UUID.randomUUID());
      entry.setEmail(dto.email());
      entry.setUserId(userId);
      entry.setShardIndex(shardIndex);
      directoryRepository.save(entry);
    } catch (RuntimeException error) {
      log.error(
          "Directory write failed for {} (userId={}) - compensating by removing shard {} rows",
          dto.email(),
          userId,
          shardIndex,
          error);
      final UUID compensateUserId = userId;
      shardRouter
          .getTransactionTemplate(shardIndex)
          .execute(
              status -> {
                WalletRepository walletRepo = shardRouter.getRepository(WalletRepository.class, shardIndex);
                UserRepository userRepo = shardRouter.getRepository(UserRepository.class, shardIndex);
                walletRepo.deleteByUserId(compensateUserId);
                userRepo.deleteById(compensateUserId);
                return null;
              });
      throw error;
    }

    String accessToken = jwtService.issue(new JwtPayload(userId.toString(), dto.email()));
    return new AuthResponse(accessToken, new AuthUserSummary(userId, dto.email(), dto.fullName()));
  }

  /**
   * Login can't compute the shard from anything it's handed (email isn't
   * the shard key, userId is) - it has to ask the directory first. This is
   * the one read in the whole app that goes through the unsharded lookup
   * table instead of straight to a shard.
   */
  public AuthResponse login(LoginRequest dto) {
    UserDirectory entry =
        directoryRepository.findByEmail(dto.email()).orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

    UserRepository userRepo = shardRouter.getRepository(UserRepository.class, entry.getUserId().toString());
    User user =
        userRepo.findById(entry.getUserId()).orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

    if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid credentials");
    }

    String accessToken = jwtService.issue(new JwtPayload(user.getId().toString(), user.getEmail()));
    return new AuthResponse(accessToken, new AuthUserSummary(user.getId(), user.getEmail(), user.getFullName()));
  }
}
