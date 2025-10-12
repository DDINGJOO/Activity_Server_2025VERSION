package com.teambind.activityserver.domain.board.service;

import com.teambind.activityserver.domain.board.dto.ArticleCursorDto;
import com.teambind.activityserver.domain.board.dto.FeedRequest;
import com.teambind.activityserver.domain.board.dto.FeedResponse;
import com.teambind.activityserver.domain.board.repository.UserArticleRepository;
import com.teambind.activityserver.domain.board.repository.UserCommentRepository;
import com.teambind.activityserver.domain.board.repository.UserLikeRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActivityFeedService {
	
	private final UserArticleRepository userArticleRepository;
	private final UserLikeRepository userLikeRepository;
	private final UserCommentRepository userCommentRepository;
	
	public ActivityFeedService(UserArticleRepository userArticleRepository,
	                           UserLikeRepository userLikeRepository,
	                           UserCommentRepository userCommentRepository) {
		this.userArticleRepository = userArticleRepository;
		this.userLikeRepository = userLikeRepository;
		this.userCommentRepository = userCommentRepository;
	}
	
	/**
	 * Main entry: returns totals (if cursor==null) and merged article ids with nextCursor.
	 */
	public FeedResponse getFeed(FeedRequest req) {
		Long targetUserId = req.getTargetUserId();
		if (targetUserId == null) {
			throw new IllegalArgumentException("targetUserId must be provided");
		}
		
		boolean newest = !"oldest".equalsIgnoreCase(req.getSort());
		// parse cursor
		Cursor cursor = parseCursor(req.getCursor());
		
		// categories to fetch
		List<String> categories = (req.getCategories() == null || req.getCategories().isEmpty())
				? List.of("article", "comment", "like")
				: req.getCategories().stream().map(String::toLowerCase).collect(Collectors.toList());
		
		// PageRequest: we fetch extra from each category to allow merging; multiply by categories size
		int size = Math.max(1, req.getSize());
		int perCategoryFetch = size * Math.max(1, categories.size());
		
		PageRequest pageable = PageRequest.of(0, perCategoryFetch);
		
		Map<String, Long> totals = new HashMap<>();
		List<ArticleCursorDto> merged = new ArrayList<>();
		
		for (String cat : categories) {
			switch (cat) {
				case "article":
					if (cursor == null) {
						totals.put("article", userArticleRepository.countByIdUserId(targetUserId));
					}
					merged.addAll(fetchFromArticleRepo(targetUserId, cursor, newest, pageable));
					break;
				case "comment":
					if (cursor == null) {
						totals.put("comment", userCommentRepository.countByIdUserId(targetUserId));
					}
					merged.addAll(fetchFromCommentRepo(targetUserId, cursor, newest, pageable));
					break;
				case "like":
					// for a viewer seeing others' feed, likes may be excluded from categories passed by caller
					if (cursor == null) {
						totals.put("like", userLikeRepository.countByIdUserId(targetUserId));
					}
					merged.addAll(fetchFromLikeRepo(targetUserId, cursor, newest, pageable));
					break;
				default:
					// ignore unknown category
			}
		}
		
		// Merge by createdAt and articleId respecting sort order
		Comparator<ArticleCursorDto> comparator = Comparator
				.comparing(ArticleCursorDto::getCreatedAt)
				.thenComparing(ArticleCursorDto::getArticleId);
		if (newest) comparator = comparator.reversed();
		
		// de-duplicate article ids while preserving order after sorting
		List<ArticleCursorDto> sorted = merged.stream()
				.sorted(comparator)
				.collect(Collectors.toList());
		
		LinkedHashMap<Long, ArticleCursorDto> deduped = new LinkedHashMap<>();
		for (ArticleCursorDto dto : sorted) {
			deduped.putIfAbsent(dto.getArticleId(), dto);
		}
		
		List<ArticleCursorDto> finalList = new ArrayList<>(deduped.values());
		// trim to requested size
		List<ArticleCursorDto> pageList = finalList.stream().limit(size).collect(Collectors.toList());
		
		// build nextCursor from last element if more elements exist
		String nextCursor = null;
		if (pageList.size() > 0) {
			ArticleCursorDto last = pageList.get(pageList.size() - 1);
			boolean hasMore = finalList.size() > pageList.size();
			if (hasMore) {
				nextCursor = encodeCursor(last.getCreatedAt(), last.getArticleId());
			}
		}
		
		List<Long> articleIds = pageList.stream().map(ArticleCursorDto::getArticleId).collect(Collectors.toList());
		return new FeedResponse(totals.isEmpty() ? null : totals, articleIds, nextCursor);
	}
	
	private List<ArticleCursorDto> fetchFromArticleRepo(Long userId, Cursor cursor, boolean newest, PageRequest pageable) {
		if (newest) {
			return userArticleRepository.fetchDescByUserIdWithCursor(userId,
					cursor == null ? null : cursor.createdAt,
					cursor == null ? null : cursor.articleId,
					pageable);
		} else {
			return userArticleRepository.fetchAscByUserIdWithCursor(userId,
					cursor == null ? null : cursor.createdAt,
					cursor == null ? null : cursor.articleId,
					pageable);
		}
	}
	
	private List<ArticleCursorDto> fetchFromLikeRepo(Long userId, Cursor cursor, boolean newest, PageRequest pageable) {
		if (newest) {
			return userLikeRepository.fetchDescByUserIdWithCursor(userId,
					cursor == null ? null : cursor.createdAt,
					cursor == null ? null : cursor.articleId,
					pageable);
		} else {
			return userLikeRepository.fetchAscByUserIdWithCursor(userId,
					cursor == null ? null : cursor.createdAt,
					cursor == null ? null : cursor.articleId,
					pageable);
		}
	}
	
	private List<ArticleCursorDto> fetchFromCommentRepo(Long userId, Cursor cursor, boolean newest, PageRequest pageable) {
		if (newest) {
			return userCommentRepository.fetchDescByUserIdWithCursor(userId,
					cursor == null ? null : cursor.createdAt,
					cursor == null ? null : cursor.articleId,
					pageable);
		} else {
			return userCommentRepository.fetchAscByUserIdWithCursor(userId,
					cursor == null ? null : cursor.createdAt,
					cursor == null ? null : cursor.articleId,
					pageable);
		}
	}
	
	private Cursor parseCursor(String cursorStr) {
		if (cursorStr == null || cursorStr.isBlank()) return null;
		// expected format: createdAt|articleId (ISO_LOCAL_DATE_TIME|id)
		try {
			String[] parts = cursorStr.split("\\|", 2);
			if (parts.length != 2) return null;
			LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
			Long articleId = Long.parseLong(parts[1]);
			return new Cursor(createdAt, articleId);
		} catch (DateTimeParseException | NumberFormatException ex) {
			return null;
		}
	}
	
	private String encodeCursor(LocalDateTime createdAt, Long articleId) {
		return createdAt.toString() + "|" + articleId;
	}
	
	private static class Cursor {
		LocalDateTime createdAt;
		Long articleId;
		
		Cursor(LocalDateTime createdAt, Long articleId) {
			this.createdAt = createdAt;
			this.articleId = articleId;
		}
	}
}
