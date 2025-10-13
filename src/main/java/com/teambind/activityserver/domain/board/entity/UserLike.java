package com.teambind.activityserver.domain.board.entity;

import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "user_like")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserLike {
	@EmbeddedId
	private UserArticleKey id;
	
	
	@Column(name = "title")
	private String title;
	
	@Column(name = "version")
	private int version;
	@Column(name = "created_at")
	private LocalDateTime createdAt;
	
	public UserLike(UserArticleKey id) {
		this.id = id;
		this.version = -0;
	}
}
