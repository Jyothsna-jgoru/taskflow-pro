package com.taskflowpro.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
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
    var cacheObjectMapper = objectMapper.copy();
    cacheObjectMapper.activateDefaultTyping(
        BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.taskflowpro.")
            .allowIfSubType("java.lang.")
            .allowIfSubType("java.time.")
            .allowIfSubType("java.util.")
            .build(),
        ObjectMapper.DefaultTyping.EVERYTHING,
        JsonTypeInfo.As.PROPERTY);
    RedisSerializationContext.SerializationPair<Object> values =
        RedisSerializationContext.SerializationPair.fromSerializer(
            new GenericJackson2JsonRedisSerializer(cacheObjectMapper));
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
