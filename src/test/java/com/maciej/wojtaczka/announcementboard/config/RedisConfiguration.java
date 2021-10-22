package com.maciej.wojtaczka.announcementboard.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Configuration
public class RedisConfiguration {

	private final RedisServer redisServer;

	public RedisConfiguration(RedisProperties redisProperties) {
		this.redisServer = new RedisServer(redisProperties.getPort());
	}

	@PostConstruct
	public void postConstruct() {
		redisServer.start();
	}

	@PreDestroy
	public void preDestroy() {
		redisServer.stop();
	}
}
