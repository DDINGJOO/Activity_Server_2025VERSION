package com.teambind.activityserver.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreatedEvent {
	private String writerId;
	private String articleId;
	private LocalDateTime createdAt;
}
