package com.teambind.activityserver.domain.board.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FeedRequest {
	// 요청하는 사용자(조회 시의 주체)
	private Long viewerId;
	// 피드를 볼 대상 사용자 (mini feed: target == viewer)
	private Long targetUserId;
	// 카테고리들: any of ["like","comment","article"]
	private List<String> categories;
	// cursor encoding: "createdAt|articleId" (예: 2025-10-12T12:00:00|123)
	private String cursor;
	// 페이지 크기
	private int size = 20;
	// sort: "newest" or "oldest"
	private String sort = "newest";
	
}
