package com.teambind.activityserver.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.activityserver.domain.article.service.ArticleSyncQueue;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.entity.UserComment;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import com.teambind.activityserver.domain.board.repository.UserArticleRepository;
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
	private final UserArticleRepository userArticleRepository;
	private final ArticleSyncQueue articleSyncQueue;
	
	@KafkaListener(topics = "comment-created", groupId = "activity-consumer-group")
	public void increaseCommentRequest(String message) throws JsonProcessingException {
		CommentCreatedEvent request = objectMapper.readValue(message, CommentCreatedEvent.class);
		UserArticleKey key = new UserArticleKey(request.getWriterId(), request.getArticleId());
		
		// 1번의 조회로 존재 여부와 데이터 취득을 동시에
		userCommentRepository.findById(key).ifPresentOrElse(
				existing -> {
				},
				() -> {
					// 댓글 저장 (articleSynced = false)
					userCommentRepository.save(new UserComment(key));
					
					// 아티클 정보 없으면 동기화 큐에 추가
					UserArticleKey articleKey = new UserArticleKey(request.getWriterId(), request.getArticleId());
					if (!userArticleRepository.existsById(articleKey)) {
						articleSyncQueue.addMissingArticle(request.getArticleId());
					}
					
					// Dirty Checking 활용 (save 생략 가능)
					UserBoardActivitiesCount userBoardActivitiesCount = userBoardActivitiesCountRepository.findById(request.getWriterId()).orElse(new UserBoardActivitiesCount(request.getWriterId()));
					userBoardActivitiesCount.increaseCommentCount();
					userBoardActivitiesCountRepository.save(userBoardActivitiesCount);
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
