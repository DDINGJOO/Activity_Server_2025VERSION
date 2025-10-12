package com.teambind.activityserver.event.consume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.repository.UserBoardActivitiesCountRepository;
import com.teambind.activityserver.event.events.ProfileCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Slf4j
@RequiredArgsConstructor
@Component
public class boardConsumeHandler {
	private final ObjectMapper objectMapper;
	private final UserBoardActivitiesCountRepository userBoardActivitiesCountRepository;
	
	
	@KafkaListener(topics = "profile-create-request", groupId = "profile-consumer-group")
	public void createUserProfile(String message) {
		log.info("Received message: {}", message);
		try {
			
			ProfileCreateRequest request = objectMapper.readValue(message, ProfileCreateRequest.class);
			userBoardActivitiesCountRepository.save(new UserBoardActivitiesCount(request.getUserId()));
			log.info("Successfully processed profile-create-request , userId : {}", request.getUserId());
			
		} catch (Exception e) {
			// 역직렬화 실패 또는 처리 중 오류 발생 시 로깅/대응
			log.error("Failed to deserialize or process profile-create-request message: {}", message, e);
			// 필요하면 DLQ 전송이나 재시도 로직 추가
		}
	}
}
