package com.teambind.activityserver.messaging.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.repository.UserBoardActivitiesCountRepository;
import com.teambind.activityserver.messaging.event.ProfileCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProfileEventConsumer {
	private final ObjectMapper objectMapper;
	private final UserBoardActivitiesCountRepository userBoardActivitiesCountRepository;
	
	@KafkaListener(topics = "user-created", groupId = "activity-consumer-group")
	public void createUserProfile(String message) throws JsonProcessingException {
		ProfileCreateRequest request = objectMapper.readValue(message, ProfileCreateRequest.class);
		userBoardActivitiesCountRepository.save(new UserBoardActivitiesCount(request.getUserId()));
	}
}
