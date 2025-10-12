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
	
	
	@Column(name = "created_at")
	private LocalDateTime createdAt;
}
