package com.monglepick.monglepickbackend.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 캐시 설정.
 *
 * <p>변경 빈도가 낮고 조회 빈도가 높은 데이터를 Redis에 캐싱하여
 * DB 부하를 줄인다.</p>
 *
 * <h3>캐시 목록</h3>
 * <ul>
 *   <li>{@code subscriptionPlans} — 구독 상품 목록 (TTL 24시간)</li>
 *   <li>{@code pointItems} — 포인트 아이템 목록 (TTL 12시간)</li>
 * </ul>
 *
 * @see com.monglepick.monglepickbackend.domain.payment.service.SubscriptionService
 * @see com.monglepick.monglepickbackend.domain.reward.service.PointItemService
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Redis 캐시 매니저 빈.
     *
     * <p>캐시별 TTL을 개별 설정하고, JSON 직렬화를 사용한다.
     * 기본 TTL은 1시간이며, 캐시별로 오버라이드 가능하다.</p>
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        /* 기본 캐시 설정: TTL 1시간, JSON 직렬화 */
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()))
                .disableCachingNullValues();

        /* 캐시별 TTL 개별 설정 */
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                /* 구독 상품 목록: 거의 변하지 않으므로 24시간 캐싱 */
                "subscriptionPlans", defaultConfig.entryTtl(Duration.ofHours(24)),
                /* 포인트 아이템 목록: 드물게 변경되므로 12시간 캐싱 */
                "pointItems", defaultConfig.entryTtl(Duration.ofHours(12))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
