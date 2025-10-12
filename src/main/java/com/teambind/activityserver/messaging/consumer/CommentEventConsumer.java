package com.teambind.activityserver.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.entity.UserComment;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import com.teambind.activityserver.domain.board.repository.UserBoardActivitiesCountRepository;
import com.teambind.activityserver.domain.board.repository.UserCommentRepository;
import com.teambind.activityserver.messaging.event.CommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CommentEventConsumer {
	private final ObjectMapper objectMapper;
	private final UserBoardActivitiesCountRepository userBoardActivitiesCountRepository;
	private final UserCommentRepository userCommentRepository;
	
	@KafkaListener(topics = "comment-created", groupId = "activity-consumer-group")
	public void increaseCommentRequest(String message) {
		log.info("Received message: {}", message);
		try {
			CommentCreatedEvent request = objectMapper.readValue(message, CommentCreatedEvent.class);
			UserArticleKey key = new UserArticleKey(request.getWriterId(), request.getArticleId());
			
			// 1번의 조회로 존재 여부와 데이터 취득을 동시에
			userCommentRepository.findById(key).ifPresentOrElse(
					existing -> log.info("Comment already exists for userId: {}, articleId: {}",
							request.getWriterId(), request.getArticleId()),
					() -> {
						userCommentRepository.save(new UserComment(request.getCreatedAt(), key));
						// Dirty Checking 활용 (save 생략 가능)
						userBoardActivitiesCountRepository.findById(request.getWriterId())
								.ifPresent(UserBoardActivitiesCount::increaseCommentCount);
					}
			);
			
			log.info("Successfully processed comment-created, userId: {}", request.getWriterId());
		} catch (Exception e) {
			log.error("Failed to process comment-created, message: {}", message, e);
		}
	}
	
	@KafkaListener(topics = "comment-deleted", groupId = "activity-consumer-group")
	public void decreaseCommentRequest(String message) {
		log.info("Received message: {}", message);
		try {
			CommentCreatedEvent request = objectMapper.readValue(message, CommentCreatedEvent.class);
			UserArticleKey key = new UserArticleKey(request.getWriterId(), request.getArticleId());
			
			userCommentRepository.findById(key).ifPresent(comment -> {
				userCommentRepository.delete(comment);
				userBoardActivitiesCountRepository.findById(request.getWriterId())
						.ifPresent(UserBoardActivitiesCount::decreaseCommentCount); // 수정됨
			});
			
			log.info("Successfully processed comment-deleted, userId: {}", request.getWriterId());
		} catch (Exception e) {
			log.error("Failed to process comment-deleted, message: {}", message, e);
		}
	}
}
