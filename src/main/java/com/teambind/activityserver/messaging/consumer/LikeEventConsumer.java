package com.teambind.activityserver.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.activityserver.domain.article.service.ArticleSyncQueue;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.entity.UserLike;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import com.teambind.activityserver.domain.board.repository.UserArticleRepository;
import com.teambind.activityserver.domain.board.repository.UserBoardActivitiesCountRepository;
import com.teambind.activityserver.domain.board.repository.UserLikeRepository;
import com.teambind.activityserver.messaging.event.LikeCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LikeEventConsumer {
	private final ObjectMapper objectMapper;
	private final UserBoardActivitiesCountRepository userBoardActivitiesCountRepository;
	private final UserLikeRepository userLikeRepository;
	private final UserArticleRepository userArticleRepository;
	private final ArticleSyncQueue articleSyncQueue;
	
	@KafkaListener(topics = "like-created", groupId = "activity-consumer-group")
	public void increaseLikeRequest(String message) throws JsonProcessingException {
		LikeCreatedEvent request = objectMapper.readValue(message, LikeCreatedEvent.class);
		UserArticleKey key = new UserArticleKey(request.getLikerId(), request.getArticleId());
		
		// 1번의 조회로 존재 여부와 데이터 취득을 동시에
		userLikeRepository.findById(key).ifPresentOrElse(
				existing -> {
				},
				() -> {
					// 좋아요 저장 (articleSynced = false)
					userLikeRepository.save(new UserLike(key));
					
					// 아티클 정보 없으면 동기화 큐에 추가
					UserArticleKey articleKey = new UserArticleKey(request.getLikerId(), request.getArticleId());
					if (!userArticleRepository.existsById(articleKey)) {
						articleSyncQueue.addMissingArticle(request.getArticleId());
					}
					
					// Dirty Checking 활용 (save 생략 가능)
					UserBoardActivitiesCount activitiesCount = userBoardActivitiesCountRepository.findById(request.getLikerId()).orElse(new UserBoardActivitiesCount(request.getLikerId()));
					activitiesCount.increaseLikeCount();
					userBoardActivitiesCountRepository.save(activitiesCount);

				}
		);
	}
	
	@KafkaListener(topics = "like-deleted", groupId = "activity-consumer-group")
	public void decreaseLikeRequest(String message) throws JsonProcessingException {
		LikeCreatedEvent request = objectMapper.readValue(message, LikeCreatedEvent.class);
		UserArticleKey key = new UserArticleKey(request.getLikerId(), request.getArticleId());
		
		userLikeRepository.findById(key).ifPresent(like -> {
			userLikeRepository.delete(like);
			userBoardActivitiesCountRepository.findById(request.getLikerId())
					.ifPresent(UserBoardActivitiesCount::decreaseLikeCount);
		});
	}
}
