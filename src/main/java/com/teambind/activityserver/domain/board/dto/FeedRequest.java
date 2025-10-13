package com.teambind.activityserver.domain.board.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 피드 관련 요청 DTO (도메인 레벨)
 * - viewerId, targetUserId는 문자열(String) 타입으로 처리합니다.
 */
@Getter
@Setter
public class FeedRequest {
	// 요청하는 사용자(조회 시의 주체)
	private String viewerId;
	// 피드를 볼 대상 사용자 (mini feed: target == viewer)
	private String targetUserId;
	// 카테고리들: any of ["like","comment","article"]
	private List<String> categories;
	// cursor: articleId 문자열 (예: "123") 또는 null
	private String cursor;
	// 페이지 크기
	private int size = 20;
	// 정렬: "newest" (기본) 또는 "oldest"
	private String sort = "newest";
	
	
}
