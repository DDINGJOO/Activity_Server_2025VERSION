package com.teambind.activityserver.domain.board.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * 피드 응답 DTO (도메인 레벨)
 * - 초기 요청(총계)에서는 totals + isOwner 사용
 * - 카테고리 요청(페이징)에서는 articleIds + nextCursor 사용
 * - articleId는 문자열(String) 타입입니다.
 */
@Setter
@Getter
public class FeedResponse {
	// 카테고리별 총합 (초기 요청시 사용)
	private Map<String, Long> totals;
	// 카테고리 페이징 결과: articleId 목록 (String)
	private List<String> articleIds;
	// 다음 페이지를 위한 커서 (articleId 문자열)
	private String nextCursor;
	// viewer가 target과 동일한지 여부 (mini-feed 여부)
	private boolean isOwner;
	
	public FeedResponse() {
	}
	
	// 초기 counts 응답 생성자
	public FeedResponse(Map<String, Long> totals, boolean isOwner) {
		this.totals = totals;
		this.isOwner = isOwner;
	}
	
	// 카테고리 페이징 응답 생성자
	public FeedResponse(List<String> articleIds, String nextCursor) {
		this.articleIds = articleIds;
		this.nextCursor = nextCursor;
	}
	
}
