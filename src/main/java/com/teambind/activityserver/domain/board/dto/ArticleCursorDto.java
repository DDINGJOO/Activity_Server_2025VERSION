package com.teambind.activityserver.domain.board.dto;

import java.time.LocalDateTime;
import java.util.Objects;

public class ArticleCursorDto {
	private final Long articleId;
	private final LocalDateTime createdAt;
	
	public ArticleCursorDto(Long articleId, LocalDateTime createdAt) {
		this.articleId = articleId;
		this.createdAt = createdAt;
	}
	
	public Long getArticleId() {
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
