package com.teambind.activityserver.domain.board.entity;


import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_article")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserArticle {
	@EmbeddedId
	private UserArticleKey id;
	
	
	@Column(name = "title")
	private String title;
	
	@Column(name = "version")
	private int version;
	@Column(name = "created_at")
	private LocalDateTime createdAt;
	
	public UserArticle(UserArticleKey key, String title, Long version, LocalDateTime createdAt) {
		this.id = key;
		this.version = version.intValue();
		this.createdAt = createdAt;
	}
}
