package com.taskflowpro.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true", matchIfMissing = true)
public class CacheConfig {
  @Bean
  RedisCacheManager cacheManager(
      RedisConnectionFactory connectionFactory,
      ObjectMapper objectMapper,
      @Value("${app.cache.dashboard-ttl:PT2M}") Duration dashboardTtl,
      @Value("${app.cache.query-ttl:PT1M}") Duration queryTtl) {
    RedisSerializationContext.SerializationPair<Object> values =
        RedisSerializationContext.SerializationPair.fromSerializer(
            GenericJackson2JsonRedisSerializer.builder()
                .objectMapper(objectMapper.copy())
                .defaultTyping(true)
                .build());
    RedisCacheConfiguration base =
        RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(values)
            .disableCachingNullValues()
            .entryTtl(queryTtl)
            .prefixCacheNameWith("taskflow:");
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(base)
        .withInitialCacheConfigurations(
            Map.of(
                "dashboard",
                base.entryTtl(dashboardTtl),
                "tasks",
                base.entryTtl(queryTtl),
                "projects",
                base.entryTtl(queryTtl)))
        .transactionAware()
        .build();
  }
}
