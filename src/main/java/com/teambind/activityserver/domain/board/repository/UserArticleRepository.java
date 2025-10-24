package com.teambind.activityserver.domain.board.repository;

import com.teambind.activityserver.domain.board.dto.ArticleCursorDto;
import com.teambind.activityserver.domain.board.entity.UserArticle;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserArticleRepository extends JpaRepository<UserArticle, UserArticleKey> {
	
	// Descending (newest first) cursor query
	@Query("select new com.teambind.activityserver.domain.board.dto.ArticleCursorDto(a.id.articleId, a.createdAt) " +
			"from UserArticle a " +
			"where a.id.userId = :userId " +
			"and (:cursorCreatedAt is null or (a.createdAt < :cursorCreatedAt or (a.createdAt = :cursorCreatedAt and a.id.articleId < :cursorArticleId))) " +
			"order by a.createdAt desc, a.id.articleId desc")
	List<ArticleCursorDto> fetchDescByUserIdWithCursor(@Param("userId") String userId,
	                                                   @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
	                                                   @Param("cursorArticleId") String cursorArticleId,
	                                                   org.springframework.data.domain.Pageable pageable);

	// Ascending (oldest first) cursor query
	@Query("select new com.teambind.activityserver.domain.board.dto.ArticleCursorDto(a.id.articleId, a.createdAt) " +
			"from UserArticle a " +
			"where a.id.userId = :userId " +
			"and (:cursorCreatedAt is null or (a.createdAt > :cursorCreatedAt or (a.createdAt = :cursorCreatedAt and a.id.articleId > :cursorArticleId))) " +
			"order by a.createdAt asc, a.id.articleId asc")
	List<ArticleCursorDto> fetchAscByUserIdWithCursor(@Param("userId") String userId,
	                                                  @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
	                                                  @Param("cursorArticleId") String cursorArticleId,
	                                                  org.springframework.data.domain.Pageable pageable);
	
	// Counts for initial request
	long countByIdUserId(String userId);
}
