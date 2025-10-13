package com.teambind.activityserver.domain.article.client;

import com.teambind.activityserver.domain.article.dto.ArticleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleClient {
	private final WebClient webClient;
	
	@Value("${article.server.url}")
	private String articleServerUrl;
	
	/**
	 * 아티클 서버에서 여러 아티클을 배치로 조회
	 * GET /api/v1/bulk/articles?ids=1&ids=2&ids=3
	 */
	public List<ArticleDto> getArticlesByIds(List<String> articleIds) {
		if (articleIds == null || articleIds.isEmpty()) {
			return Collections.emptyList();
		}
		
		try {
			return webClient.get()
					.uri(uriBuilder -> {
						uriBuilder.path("/api/v1/bulk/articles");
						articleIds.forEach(id -> uriBuilder.queryParam("ids", id));
						return uriBuilder.build();
					})
					.retrieve()
					.bodyToFlux(ArticleDto.class)
					.collectList()
					.timeout(Duration.ofSeconds(10))
					.onErrorResume(e -> {
						log.error("아티클 배치 조회 실패: ids={}, error={}", articleIds, e.getMessage());
						return Mono.just(Collections.emptyList());
					})
					.block();
		} catch (Exception e) {
			log.error("아티클 배치 조회 예외: ids={}", articleIds, e);
			return Collections.emptyList();
		}
	}
}
