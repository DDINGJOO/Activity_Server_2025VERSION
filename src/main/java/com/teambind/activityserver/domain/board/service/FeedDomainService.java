package com.teambind.activityserver.domain.board.service;

import com.teambind.activityserver.domain.board.dto.ArticleCursorDto;
import com.teambind.activityserver.domain.board.dto.CategoryPage;
import com.teambind.activityserver.domain.board.entity.UserArticle;
import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import com.teambind.activityserver.domain.board.repository.UserArticleRepository;
import com.teambind.activityserver.domain.board.repository.UserBoardActivitiesCountRepository;
import com.teambind.activityserver.domain.board.repository.UserCommentRepository;
import com.teambind.activityserver.domain.board.repository.UserLikeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 도메인 서비스: 피드 관련 핵심 비즈니스 로직을 담당합니다.
 * - 초기 요청에서 집계 테이블을 우선 사용해 totals 제공
 * - 카테고리별 articleId cursor 페이징 제공
 * <p>
 * 모든 userId와 articleId는 문자열(String) 타입으로 처리합니다.
 */
@Service
public class FeedDomainService {
	
	private final UserArticleRepository userArticleRepository;
	private final UserLikeRepository userLikeRepository;
	private final UserCommentRepository userCommentRepository;
	private final UserBoardActivitiesCountRepository userBoardActivitiesCountRepository;
	
	public FeedDomainService(UserArticleRepository userArticleRepository,
	                         UserLikeRepository userLikeRepository,
	                         UserCommentRepository userCommentRepository,
	                         UserBoardActivitiesCountRepository userBoardActivitiesCountRepository) {
		this.userArticleRepository = userArticleRepository;
		this.userLikeRepository = userLikeRepository;
		this.userCommentRepository = userCommentRepository;
		this.userBoardActivitiesCountRepository = userBoardActivitiesCountRepository;
	}
	
	/**
	 * 요청된 카테고리에 대한 총합을 계산하여 반환합니다.
	 * 우선 user_board_activities_count 테이블(집계)을 사용하며, 없을 경우 폴백으로 COUNT 쿼리를 수행합니다.
	 */
	public Map<String, Long> computeTotals(String targetUserId, List<String> categories) {
		Map<String, Long> totals = new HashMap<>();
		if (targetUserId == null) return totals;
		
		Optional<UserBoardActivitiesCount> aggOpt =
				userBoardActivitiesCountRepository.findById(targetUserId);
		
		for (String cat : categories) {
			switch (cat) {
				case "article":
					if (aggOpt.isPresent()) {
						Integer v = aggOpt.get().getTotalArticleCount();
						totals.put("article", v == null ? 0L : v.longValue());
					} else {
						totals.put("article", userArticleRepository.countByIdUserId(targetUserId));
					}
					break;
				case "comment":
					if (aggOpt.isPresent()) {
						Integer v = aggOpt.get().getTotalCommentCount();
						totals.put("comment", v == null ? 0L : v.longValue());
					} else {
						totals.put("comment", userCommentRepository.countByIdUserId(targetUserId));
					}
					break;
				case "like":
					if (aggOpt.isPresent()) {
						Integer v = aggOpt.get().getTotalLikeCount();
						totals.put("like", v == null ? 0L : v.longValue());
					} else {
						totals.put("like", userLikeRepository.countByIdUserId(targetUserId));
					}
					break;
				default:
					// 알 수 없는 카테고리는 무시
			}
		}
		return totals;
	}
	
	/**
	 * 단일 카테고리에 대해 articleId 목록과 nextCursor를 반환합니다.
	 * cursor: articleId 문자열 (예: "123"). 내부적으로 해당 articleId의 createdAt을 찾아 안정적인 페이징을 수행합니다.
	 */
	public CategoryPage fetchCategoryPage(String category, String targetUserId, String cursor, int size, String sort) {
		if (targetUserId == null) throw new IllegalArgumentException("targetUserId is required");
		
		boolean newest = !"oldest".equalsIgnoreCase(sort);
		String cursorArticleId = parseCursor(cursor);
		LocalDateTime cursorCreatedAt = resolveCursorCreatedAt(targetUserId, cursorArticleId);
		int pageSize = Math.max(1, size);
		
		PageRequest pageable = PageRequest.of(0, pageSize + 1); // 한 개 더 가져와서 hasMore 판단
		
		List<ArticleCursorDto> fetched;
		switch (category.toLowerCase()) {
			case "article":
				fetched = fetchFromArticleRepo(targetUserId, cursorCreatedAt, cursorArticleId, newest, pageable);
				break;
			case "comment":
				fetched = fetchFromCommentRepo(targetUserId, cursorCreatedAt, cursorArticleId, newest, pageable);
				break;
			case "like":
				fetched = fetchFromLikeRepo(targetUserId, cursorCreatedAt, cursorArticleId, newest, pageable);
				break;
			default:
				return new CategoryPage(Collections.emptyList(), null);
		}
		
		// 정렬 및 중복 제거 (안정성 확보)
		Comparator<ArticleCursorDto> comparator = Comparator
				.comparing(ArticleCursorDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
				.thenComparing(ArticleCursorDto::articleId, Comparator.nullsLast(Comparator.naturalOrder()));
		if (newest) comparator = comparator.reversed();
		
		List<ArticleCursorDto> sorted = new ArrayList<>(fetched);
		sorted.sort(comparator);
		
		LinkedHashMap<String, ArticleCursorDto> dedup = new LinkedHashMap<>();
		for (ArticleCursorDto dto : sorted) {
			dedup.putIfAbsent(dto.articleId(), dto);
		}
		List<ArticleCursorDto> dedupList = new ArrayList<>(dedup.values());
		
		boolean hasMore = dedupList.size() > pageSize;
		List<ArticleCursorDto> pageList = dedupList.stream().limit(pageSize).toList();
		
		String nextCursor = null;
		if (hasMore && !pageList.isEmpty()) {
			ArticleCursorDto last = pageList.get(pageList.size() - 1);
			nextCursor = encodeCursor(last.articleId());
		}
		
		List<String> articleIds = pageList.stream().map(ArticleCursorDto::articleId).toList();
		return new CategoryPage(articleIds, nextCursor);
	}
	
	// Repository 호출 래퍼들
	private List<ArticleCursorDto> fetchFromArticleRepo(String userId,
	                                                    LocalDateTime cursorCreatedAt, String cursorArticleId,
	                                                    boolean newest, PageRequest pageable) {
		if (newest) {
			return userArticleRepository.fetchDescByUserIdWithCursor(userId, cursorCreatedAt, cursorArticleId, pageable);
		} else {
			return userArticleRepository.fetchAscByUserIdWithCursor(userId, cursorCreatedAt, cursorArticleId, pageable);
		}
	}
	
	private List<ArticleCursorDto> fetchFromLikeRepo(String userId,
	                                                 LocalDateTime cursorCreatedAt, String cursorArticleId,
	                                                 boolean newest, PageRequest pageable) {
		if (newest) {
			return userLikeRepository.fetchDescByUserIdWithCursor(userId, cursorCreatedAt, cursorArticleId, pageable);
		} else {
			return userLikeRepository.fetchAscByUserIdWithCursor(userId, cursorCreatedAt, cursorArticleId, pageable);
		}
	}
	
	private List<ArticleCursorDto> fetchFromCommentRepo(String userId,
	                                                    LocalDateTime cursorCreatedAt, String cursorArticleId,
	                                                    boolean newest, PageRequest pageable) {
		if (newest) {
			return userCommentRepository.fetchDescByUserIdWithCursor(userId, cursorCreatedAt, cursorArticleId, pageable);
		} else {
			return userCommentRepository.fetchAscByUserIdWithCursor(userId, cursorCreatedAt, cursorArticleId, pageable);
		}
	}
	
	/**
	 * (userId, articleId) 조합으로 createdAt을 찾아 반환합니다.
	 * article 레코드가 먼저 존재하면 그 값을 사용하고, 없으면 comment/like를 차례로 확인합니다.
	 * 찾지 못하면 null을 반환하여 최신부터 조회하도록 합니다.
	 */
	private LocalDateTime resolveCursorCreatedAt(String userId, String articleId) {
		if (articleId == null) return null;
		UserArticleKey key = new UserArticleKey(userId, articleId);
		return userArticleRepository.findById(key).map(UserArticle::getCreatedAt)
				.or(() -> userCommentRepository.findById(key).map(c -> c.getCreatedAt()))
				.or(() -> userLikeRepository.findById(key).map(l -> l.getCreatedAt()))
				.orElse(null);
	}
	
	private String parseCursor(String cursorStr) {
		if (cursorStr == null || cursorStr.isBlank()) return null;
		return cursorStr;
	}
	
	private String encodeCursor(String articleId) {
		return articleId;
	}
}
