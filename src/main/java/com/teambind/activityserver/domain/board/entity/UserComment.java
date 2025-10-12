package com.teambind.activityserver.domain.board.entity;


import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_comment")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserComment {
	
	@Column(name = "created_at")
	LocalDateTime createdAt;
	@EmbeddedId
	private UserArticleKey id;
}
