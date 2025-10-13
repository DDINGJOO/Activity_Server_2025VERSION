package com.teambind.activityserver.messaging.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCreatedEvent {
	private String articleId;
	private String writerId;
	private Long version;
	private String title;
	private LocalDateTime createdAt;
	
}
