package com.mall.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;

/** Test-only stand-in for Redis excluded by src/test/resources/application.yml. */
@Configuration
public class TestInfrastructureConfiguration {
    @Bean
    public StringRedisTemplate stringRedisTemplate() {
        return mock(StringRedisTemplate.class);
    }
}
