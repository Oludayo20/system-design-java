package com.systemdesign.ecommarketplace.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Mirrors src/infrastructure/redis/redis.service.ts. Thin wrapper used for
 * cache-aside (Marketplace product listing) and, per doc.md's list of Redis
 * use cases, would also back sessions/OTP/rate limiting if those modules
 * were in scope for this demo.
 *
 * <p>Values are stored as JSON strings (via the app's shared, Boot-managed
 * ObjectMapper, which already has the JavaTimeModule registered for
 * OffsetDateTime fields), same as ioredis + JSON.stringify/parse in the
 * original.
 */
@Service
public class RedisService {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  public RedisService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    this.redisTemplate = redisTemplate;
    this.objectMapper = objectMapper;
  }

  public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
    String raw = redisTemplate.opsForValue().get(key);
    if (raw == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(raw, typeReference));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize redis value for key " + key, e);
    }
  }

  public void set(String key, Object value, long ttlSeconds) {
    try {
      String json = objectMapper.writeValueAsString(value);
      redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize redis value for key " + key, e);
    }
  }

  public void delete(String key) {
    redisTemplate.delete(key);
  }

  public String ping() {
    var connectionFactory = redisTemplate.getConnectionFactory();
    if (connectionFactory == null) {
      throw new IllegalStateException("No Redis connection factory configured");
    }
    return connectionFactory.getConnection().ping();
  }
}
