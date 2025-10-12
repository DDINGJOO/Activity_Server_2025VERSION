package com.teambind.activityserver.domain.board.repository;

import com.teambind.activityserver.domain.board.dto.ArticleCursorDto;
import com.teambind.activityserver.domain.board.entity.UserLike;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserLikeRepository extends JpaRepository<UserLike, UserArticleKey> {
	
	// Descending (newest first) cursor query for likes by user (liker)
	@Query("select new com.teambind.activityserver.messaging.dto.ArticleCursorDto(l.id.articleId, l.createdAt) " +
			"from UserLike l " +
			"where l.id.userId = :userId " +
			"and (:cursorCreatedAt is null or (l.createdAt < :cursorCreatedAt or (l.createdAt = :cursorCreatedAt and l.id.articleId < :cursorArticleId))) " +
			"order by l.createdAt desc, l.id.articleId desc")
	List<ArticleCursorDto> fetchDescByUserIdWithCursor(@Param("userId") Long userId,
	                                                   @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
	                                                   @Param("cursorArticleId") Long cursorArticleId,
	                                                   Pageable pageable);
	
	// Ascending (oldest first) cursor query for likes by user (liker)
	@Query("select new com.teambind.activityserver.messaging.dto.ArticleCursorDto(l.id.articleId, l.createdAt) " +
			"from UserLike l " +
			"where l.id.userId = :userId " +
			"and (:cursorCreatedAt is null or (l.createdAt > :cursorCreatedAt or (l.createdAt = :cursorCreatedAt and l.id.articleId > :cursorArticleId))) " +
			"order by l.createdAt asc, l.id.articleId asc")
	List<ArticleCursorDto> fetchAscByUserIdWithCursor(@Param("userId") Long userId,
	                                                  @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
	                                                  @Param("cursorArticleId") Long cursorArticleId,
	                                                  Pageable pageable);
	
	long countByIdUserId(Long userId);
}
