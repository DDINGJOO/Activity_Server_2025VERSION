package com.teambind.activityserver.domain.article.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleSyncQueue {
	private static final String QUEUE_KEY = "sync:missing-articles";
	private final RedisTemplate<String, String> redisTemplate;
	
	/**
	 * 동기화가 필요한 아티클 ID를 큐에 추가
	 */
	public void addMissingArticle(String articleId) {
		try {
			redisTemplate.opsForSet().add(QUEUE_KEY, articleId);
			log.debug("아티클 동기화 큐에 추가: {}", articleId);
		} catch (Exception e) {
			log.error("Redis 큐 추가 실패: articleId={}", articleId, e);
		}
	}
	
	/**
	 * 큐의 모든 아티클 ID를 가져오고 삭제
	 */
	public Set<String> popAll() {
		try {
			Set<String> ids = redisTemplate.opsForSet().members(QUEUE_KEY);
			if (ids == null || ids.isEmpty()) {
				return Collections.emptySet();
			}
			
			// 가져온 후 삭제
			redisTemplate.delete(QUEUE_KEY);
			log.info("Redis 큐에서 {} 개 아티클 ID 조회", ids.size());
			return ids;
		} catch (Exception e) {
			log.error("Redis 큐 조회 실패", e);
			return Collections.emptySet();
		}
	}
	
	/**
	 * 큐의 크기 조회
	 */
	public long size() {
		try {
			Long size = redisTemplate.opsForSet().size(QUEUE_KEY);
			return size != null ? size : 0;
		} catch (Exception e) {
			log.error("Redis 큐 크기 조회 실패", e);
			return 0;
		}
	}
}
