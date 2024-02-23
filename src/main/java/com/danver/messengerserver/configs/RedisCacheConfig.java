package com.danver.messengerserver.configs;

import com.danver.messengerserver.exceptions.handlers.RedisCacheErrorHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private Process process;
    @Value("${spring.data.redis.server.start-by-app}")
    private String startRedisByApp;

    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<?, ?> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        return template;
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new RedisCacheErrorHandler();
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .prefixCacheNameWith(this.getClass().getPackageName() + ".")
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                // userDetails cache config
                .withCacheConfiguration("userDetails", userDetailsConfig())
                .build();
    }

    private RedisCacheConfiguration userDetailsConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofDays(1));
    }

    @PostConstruct
    private void startRedisServer() {
        if (! Boolean.parseBoolean(this.startRedisByApp)) return;
        log.info("Starting Redis server manually");
        String command = "redis-server";
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        ProcessBuilder builder = new ProcessBuilder();
        if (isWindows) {
            String location = "C:/Windows/system32";
            builder.directory(new File(location));
            builder.command("cmd.exe", "/c",  command);
        } else {
            builder.command("sh" , "-c", command);
        }
        try {
            process = builder.start();
        } catch (IOException e) {
            log.info("Couldn't start Redis server: " + e.getMessage());
        }
    }

    @PreDestroy
    private void stopRedisServer() {
        if (! Boolean.parseBoolean(this.startRedisByApp)) return;
        log.info("Stopping Redis server process");
        if (this.process != null && this.process.isAlive()) {
            try {
                this.process.waitFor(5, TimeUnit.SECONDS);
                this.process.destroy();
            } catch (InterruptedException e) {
                log.info("Couldn't stop Redis server: " + e.getMessage());
            }
        }
    }
}
