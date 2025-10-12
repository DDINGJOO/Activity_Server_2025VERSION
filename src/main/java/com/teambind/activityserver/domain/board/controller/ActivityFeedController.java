package com.teambind.activityserver.domain.board.controller;

import com.teambind.activityserver.domain.board.dto.FeedRequest;
import com.teambind.activityserver.domain.board.dto.FeedResponse;
import com.teambind.activityserver.domain.board.service.ActivityFeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityFeedController {
	
	private final ActivityFeedService activityFeedService;
	
	public ActivityFeedController(ActivityFeedService activityFeedService) {
		this.activityFeedService = activityFeedService;
	}
	
	/**
	 * POST /api/activity/feed
	 * Body: FeedRequest
	 * <p>
	 * Returns FeedResponse containing totals (if initial request) and merged articleIds + nextCursor.
	 */
	@PostMapping("/feed")
	public ResponseEntity<FeedResponse> getFeed(@RequestBody FeedRequest req) {
		FeedResponse resp = activityFeedService.getFeed(req);
		return ResponseEntity.ok(resp);
	}
}
