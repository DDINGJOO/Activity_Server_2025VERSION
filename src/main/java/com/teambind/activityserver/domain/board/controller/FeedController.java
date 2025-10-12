package com.teambind.activityserver.domain.board.controller;

import com.teambind.activityserver.domain.board.dto.CategoryPage;
import com.teambind.activityserver.domain.board.dto.FeedRequest;
import com.teambind.activityserver.domain.board.dto.FeedResponse;
import com.teambind.activityserver.domain.board.service.FeedDomainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 도메인 레벨 컨트롤러: 피드 관련 엔드포인트를 제공합니다.
 * - POST /api/board/feed : 초기 counts 요청 (totals + isOwner)
 * - GET  /api/board/feed/{category} : 카테고리별 articleId 페이징 조회
 * <p>
 * 주의: "like" 카테고리는 viewer와 target이 동일한 경우에만 허용하는 등 권한 검사를 수행합니다.
 */
@RestController
@RequestMapping("/api/board")
public class FeedController {
	
	private final FeedDomainService feedDomainService;
	
	public FeedController(FeedDomainService feedDomainService) {
		this.feedDomainService = feedDomainService;
	}
	
	/**
	 * 초기 요청: 카테고리별 총합과 isOwner 플래그 반환
	 */
	@PostMapping("/feed")
	public ResponseEntity<FeedResponse> getTotals(@RequestBody FeedRequest req) {
		String targetUserId = req.getTargetUserId();
		String viewerId = req.getViewerId();
		if (targetUserId == null || targetUserId.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		
		boolean isOwner = (viewerId != null && viewerId.equals(targetUserId));
		List<String> categories = (req.getCategories() == null || req.getCategories().isEmpty())
				? List.of("article", "comment", "like")
				: req.getCategories();
		
		Map<String, Long> totals = feedDomainService.computeTotals(targetUserId, categories);
		return ResponseEntity.ok(new FeedResponse(totals.isEmpty() ? null : totals, isOwner));
	}
	
	/**
	 * 카테고리별 게시글 아이디 목록 조회
	 * - category: article | comment | like
	 * - viewerId: 조회자 (권한체크용)
	 * - targetUserId: 대상 사용자
	 * - cursor: articleId string (optional)
	 * - size: 페이지 크기 (optional)
	 * - sort: "newest" (default) or "oldest"
	 */
	@GetMapping("/feed/{category}")
	public ResponseEntity<FeedResponse> getCategoryFeed(
			@PathVariable String category,
			@RequestParam(required = false) String viewerId,
			@RequestParam String targetUserId,
			@RequestParam(required = false) String cursor,
			@RequestParam(required = false, defaultValue = "20") int size,
			@RequestParam(required = false, defaultValue = "newest") String sort
	) {
		// 좋아요 목록은 본인만 조회 가능하도록 권한 체크
		if ("like".equalsIgnoreCase(category) && (viewerId == null || !viewerId.equals(targetUserId))) {
			return ResponseEntity.status(403).build();
		}
		
		CategoryPage page = feedDomainService.fetchCategoryPage(category, targetUserId, cursor, size, sort);
		return ResponseEntity.ok(new FeedResponse(page.articleIds(), page.nextCursor()));
	}
}
