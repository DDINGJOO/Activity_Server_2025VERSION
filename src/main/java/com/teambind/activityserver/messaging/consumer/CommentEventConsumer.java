package com.teambind.activityserver.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.entity.UserComment;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import com.teambind.activityserver.domain.board.repository.UserBoardActivitiesCountRepository;
import com.teambind.activityserver.domain.board.repository.UserCommentRepository;
import com.teambind.activityserver.messaging.event.CommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CommentEventConsumer {
	private final ObjectMapper objectMapper;
	private final UserBoardActivitiesCountRepository userBoardActivitiesCountRepository;
	private final UserCommentRepository userCommentRepository;
	
	@KafkaListener(topics = "comment-created", groupId = "activity-consumer-group")
	public void increaseCommentRequest(String message) throws JsonProcessingException {
		CommentCreatedEvent request = objectMapper.readValue(message, CommentCreatedEvent.class);
		UserArticleKey key = new UserArticleKey(request.getWriterId(), request.getArticleId());
		
		// 1번의 조회로 존재 여부와 데이터 취득을 동시에
		userCommentRepository.findById(key).ifPresentOrElse(
				existing -> {
				},
				() -> {
					userCommentRepository.save(new UserComment(key));
					// Dirty Checking 활용 (save 생략 가능)
					userBoardActivitiesCountRepository.findById(request.getWriterId())
							.ifPresent(UserBoardActivitiesCount::increaseCommentCount);
				}
		);
	}
	
	@KafkaListener(topics = "comment-deleted", groupId = "activity-consumer-group")
	public void decreaseCommentRequest(String message) throws JsonProcessingException {
		CommentCreatedEvent request = objectMapper.readValue(message, CommentCreatedEvent.class);
		UserArticleKey key = new UserArticleKey(request.getWriterId(), request.getArticleId());
		
		userCommentRepository.findById(key).ifPresent(comment -> {
			userCommentRepository.delete(comment);
			userBoardActivitiesCountRepository.findById(request.getWriterId())
					.ifPresent(UserBoardActivitiesCount::decreaseCommentCount); // 수정됨
		});
	}
}
