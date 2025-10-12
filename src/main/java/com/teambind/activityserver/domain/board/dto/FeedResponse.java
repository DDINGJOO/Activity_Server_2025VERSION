package com.teambind.activityserver.domain.board.dto;

import java.util.List;
import java.util.Map;

public class FeedResponse {
	// category -> total count (only included categories)
	private Map<String, Long> totals;
	// merged article ids in requested order
	private List<Long> articleIds;
	// next cursor string (null if no more)
	private String nextCursor;
	
	public FeedResponse() {
	}
	
	public FeedResponse(Map<String, Long> totals, List<Long> articleIds, String nextCursor) {
		this.totals = totals;
		this.articleIds = articleIds;
		this.nextCursor = nextCursor;
	}
	
	public Map<String, Long> getTotals() {
		return totals;
	}
	
	public void setTotals(Map<String, Long> totals) {
		this.totals = totals;
	}
	
	public List<Long> getArticleIds() {
		return articleIds;
	}
	
	public void setArticleIds(List<Long> articleIds) {
		this.articleIds = articleIds;
	}
	
	public String getNextCursor() {
		return nextCursor;
	}
	
	public void setNextCursor(String nextCursor) {
		this.nextCursor = nextCursor;
	}
}
