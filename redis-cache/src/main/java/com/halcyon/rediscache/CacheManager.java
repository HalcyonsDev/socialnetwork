package com.halcyon.rediscache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheManager {
    private final RedisTemplate<String, Object> redisTemplate;

    public void save(String key, Object value, Duration timeToLive) {
        redisTemplate.opsForValue().set(key, value, timeToLive);
        log.info("Cached value with key '{}' for {} seconds", key, timeToLive);
    }

    public void save(String key, Duration timeToLive) {
        redisTemplate.opsForValue().set(key, "", timeToLive);
        log.info("Cached non with key '{}' for {} seconds", key, timeToLive);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean isPresent(String key) {
        Object fetchedValue = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(fetchedValue).isPresent();
    }

    public <T> Optional<T> fetch(String key, Class<T> targetClass) {
        Optional<Object> value = Optional.ofNullable(redisTemplate.opsForValue().get(key));

        if (value.isEmpty()) {
            log.info("No cached value found for key {}", key);
            return Optional.empty();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        T result = objectMapper.convertValue(value.get(), targetClass);
        log.info("Fetched cached value with key {}", key);
        return Optional.of(result);
    }
}
