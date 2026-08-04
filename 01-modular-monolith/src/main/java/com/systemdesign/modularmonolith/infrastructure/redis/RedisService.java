package com.systemdesign.modularmonolith.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Thin wrapper around Spring Data Redis's {@link StringRedisTemplate}. Kept intentionally small:
 * modules depend on this service (never on the Redis client directly) so the underlying
 * cache/session store can be swapped without touching module code.
 *
 * Mirrors {@code src/infrastructure/redis/redis.service.ts} (which wraps ioredis). The
 * connection itself is configured entirely by Spring Boot's Redis autoconfiguration from
 * {@code spring.data.redis.url} (see application.yml) -- the equivalent of {@code redis.module.ts}'s
 * {@code REDIS_CLIENT} provider factory.
 */
@Slf4j
@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public <T> T getJson(String key, Class<T> type) {
        String raw = get(key);
        if (raw == null) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse cached JSON for key \"{}\", treating as a cache miss", key);
            return null;
        }
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    public void setJson(String key, Object value) {
        try {
            set(key, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value for key \"{}\" to JSON, skipping cache write", key);
        }
    }

    public void setJson(String key, Object value, long ttlSeconds) {
        try {
            set(key, objectMapper.writeValueAsString(value), ttlSeconds);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize value for key \"{}\" to JSON, skipping cache write", key);
        }
    }

    public void del(String key) {
        redisTemplate.delete(key);
    }

    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public void expire(String key, long ttlSeconds) {
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    public StringRedisTemplate getClient() {
        return redisTemplate;
    }
}
