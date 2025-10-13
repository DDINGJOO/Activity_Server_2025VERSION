package com.teambind.activityserver.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class SchedulerConfig {
	
	/**
	 * ShedLock용 Redis 기반 분산 락 프로바이더
	 * 여러 서버 인스턴스 중 하나만 스케줄러를 실행하도록 보장
	 */
	@Bean
	public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
		return new RedisLockProvider(connectionFactory);
	}
}
