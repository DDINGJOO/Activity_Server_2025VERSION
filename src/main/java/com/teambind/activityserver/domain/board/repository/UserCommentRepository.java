package com.teambind.activityserver.domain.board.repository;

import com.teambind.activityserver.domain.board.dto.ArticleCursorDto;
import com.teambind.activityserver.domain.board.entity.UserComment;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserCommentRepository extends JpaRepository<UserComment, UserArticleKey> {
	
	// Descending (newest first) cursor query for comments by user (writer)
	@Query("select new com.teambind.activityserver.domain.board.dto.ArticleCursorDto(c.id.articleId, c.createdAt) " +
			"from UserComment c " +
			"where c.id.userId = :userId " +
			"and (:cursorCreatedAt is null or (c.createdAt < :cursorCreatedAt or (c.createdAt = :cursorCreatedAt and c.id.articleId < :cursorArticleId))) " +
			"order by c.createdAt desc, c.id.articleId desc")
	List<ArticleCursorDto> fetchDescByUserIdWithCursor(@Param("userId") String userId,
	                                                   @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
	                                                   @Param("cursorArticleId") String cursorArticleId,
	                                                   Pageable pageable);
	
	// Ascending (oldest first) cursor query for comments by user (writer)
	@Query("select new com.teambind.activityserver.domain.board.dto.ArticleCursorDto(c.id.articleId, c.createdAt) " +
			"from UserComment c " +
			"where c.id.userId = :userId " +
			"and (:cursorCreatedAt is null or (c.createdAt > :cursorCreatedAt or (c.createdAt = :cursorCreatedAt and c.id.articleId > :cursorArticleId))) " +
			"order by c.createdAt asc, c.id.articleId asc")
	List<ArticleCursorDto> fetchAscByUserIdWithCursor(@Param("userId") String userId,
	                                                  @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
	                                                  @Param("cursorArticleId") String cursorArticleId,
	                                                  Pageable pageable);
	
	long countByIdUserId(String userId);
	
	// 아티클 ID로 모든 댓글 조회 (동기화용)
	List<UserComment> findAllByIdArticleId(String articleId);
	
	// 미동기화된 아티클 ID 조회 (최근 24시간)
	@Query("SELECT DISTINCT c.id.articleId FROM UserComment c " +
			"WHERE c.articleSynced = false " +
			"AND c.createdAt > :since")
	List<String> findUnsyncedArticleIds(@Param("since") LocalDateTime since);
}
