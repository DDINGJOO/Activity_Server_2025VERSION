package com.teambind.activityserver.domain.board.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 도메인 레벨 DTO: articleId와 createdAt을 함께 전달하기 위한 객체
 * articleId는 문자열 타입(String)으로 처리합니다.
 */
public class ArticleCursorDto {
	private final String articleId;
	private final LocalDateTime createdAt;
	
	public ArticleCursorDto(String articleId, LocalDateTime createdAt) {
		this.articleId = articleId;
		this.createdAt = createdAt;
	}
	
	public String getArticleId() {
		return articleId;
	}
	
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		ArticleCursorDto that = (ArticleCursorDto) o;
		return Objects.equals(articleId, that.articleId) && Objects.equals(createdAt, that.createdAt);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(articleId, createdAt);
	}
}
