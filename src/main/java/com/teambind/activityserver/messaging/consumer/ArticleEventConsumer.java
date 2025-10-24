package com.teambind.activityserver.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.activityserver.domain.board.entity.UserArticle;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import com.teambind.activityserver.domain.board.repository.UserArticleRepository;
import com.teambind.activityserver.domain.board.repository.UserBoardActivitiesCountRepository;
import com.teambind.activityserver.messaging.event.ArticleCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ArticleEventConsumer {
	private final ObjectMapper objectMapper;
	private final UserBoardActivitiesCountRepository userBoardActivitiesCountRepository;
	private final UserArticleRepository userArticleRepository;
	
	@KafkaListener(topics = "article-created", groupId = "activity-consumer-group")
	@Transactional
	public void increaseArticleRequest(String message) throws JsonProcessingException {
		ArticleCreatedEvent request = objectMapper.readValue(message, ArticleCreatedEvent.class);
		UserArticleKey key = new UserArticleKey(request.getWriterId(), request.getArticleId());
		
		Optional<UserArticle> article = userArticleRepository.findById(key);
		if (article.isEmpty()) {
			userArticleRepository.save(new UserArticle(key, request.getTitle(), request.getVersion(), request.getCreatedAt()));
			userBoardActivitiesCountRepository.findById(request.getWriterId()).ifPresent(UserBoardActivitiesCount::increaseArticleCount);
		} else {
			if (article.get().getVersion() < request.getVersion()) {
				userArticleRepository.save(new UserArticle(key, request.getTitle(), request.getVersion(), request.getCreatedAt()));
			}
		}
		
		
	}
	
	@KafkaListener(topics = "article-deleted", groupId = "activity-consumer-group")
	public void decreaseArticleRequest(String message) throws JsonProcessingException {
		ArticleCreatedEvent request = objectMapper.readValue(message, ArticleCreatedEvent.class);
		UserArticleKey key = new UserArticleKey(request.getWriterId(), request.getArticleId());
		
		userArticleRepository.findById(key).ifPresent(article -> {
			userArticleRepository.delete(article);
			userBoardActivitiesCountRepository.findById(request.getWriterId())
					.ifPresent(UserBoardActivitiesCount::decreaseArticleCount);
		});
	}
}
