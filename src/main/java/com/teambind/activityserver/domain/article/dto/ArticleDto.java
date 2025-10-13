package com.teambind.activityserver.domain.article.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDto {
	private String articleId;
	private String title;
	private String writerId;
	private Long version;
	private LocalDateTime createdAt;
}
