package com.teambind.activityserver.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.repository.UserBoardActivitiesCountRepository;
import com.teambind.activityserver.messaging.event.ProfileCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProfileEventConsumer {
	private final ObjectMapper objectMapper;
	private final UserBoardActivitiesCountRepository userBoardActivitiesCountRepository;
	
	@KafkaListener(topics = "profile-create-request", groupId = "activity-consumer-group")
	public void createUserProfile(String message) {
		log.info("Received message: {}", message);
		try {
			ProfileCreateRequest request = objectMapper.readValue(message, ProfileCreateRequest.class);
			userBoardActivitiesCountRepository.save(new UserBoardActivitiesCount(request.getUserId()));
			log.info("Successfully processed profile-create-request , userId : {}", request.getUserId());
		} catch (Exception e) {
			log.error("Failed to deserialize or process profile-create-request message: {}", message, e);
		}
	}
}
