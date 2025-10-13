package com.teambind.activityserver.domain.article.scheduler;

import com.teambind.activityserver.domain.article.client.ArticleClient;
import com.teambind.activityserver.domain.article.dto.ArticleDto;
import com.teambind.activityserver.domain.article.service.ArticleSyncQueue;
import com.teambind.activityserver.domain.board.entity.UserArticle;
import com.teambind.activityserver.domain.board.entity.UserComment;
import com.teambind.activityserver.domain.board.entity.UserLike;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import com.teambind.activityserver.domain.board.repository.UserArticleRepository;
import com.teambind.activityserver.domain.board.repository.UserCommentRepository;
import com.teambind.activityserver.domain.board.repository.UserLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticleSyncScheduler {
	private final ArticleSyncQueue articleSyncQueue;
	private final ArticleClient articleClient;
	private final UserArticleRepository userArticleRepository;
	private final UserCommentRepository userCommentRepository;
	private final UserLikeRepository userLikeRepository;
	
	/**
	 * 1분마다 실행되는 아티클 동기화 배치
	 * - Redis 큐에서 미동기화 아티클 ID 조회 (우선)
	 * - Redis 비었으면 DB에서 최근 24시간 미동기화 데이터 조회
	 * - 아티클 서버에서 배치 조회 후 업데이트
	 */
	@Scheduled(fixedRate = 60000) // 1분마다
	@SchedulerLock(name = "syncMissingArticles", lockAtMostFor = "50s", lockAtLeastFor = "10s")
	public void syncMissingArticles() {
		try {
			// 1단계: Redis 큐에서 아티클 ID 조회
			Set<String> redisArticleIds = articleSyncQueue.popAll();
			
			// 2단계: Redis 비었으면 DB에서 미동기화 아티클 조회
			Set<String> dbArticleIds = Collections.emptySet();
			if (redisArticleIds.isEmpty()) {
				dbArticleIds = findUnsyncedArticleIdsFromDB();
			}
			
			// 합치기
			Set<String> allArticleIds = new HashSet<>();
			allArticleIds.addAll(redisArticleIds);
			allArticleIds.addAll(dbArticleIds);
			
			if (allArticleIds.isEmpty()) {
				log.debug("동기화할 아티클 없음");
				return;
			}
			
			log.info("아티클 동기화 시작: {}개 (Redis: {}, DB: {})",
					allArticleIds.size(), redisArticleIds.size(), dbArticleIds.size());
			
			// 3단계: 배치 조회 및 동기화
			int totalSynced = syncArticles(new ArrayList<>(allArticleIds));
			
			log.info("아티클 동기화 완료: {}/{}개", totalSynced, allArticleIds.size());
			
		} catch (Exception e) {
			log.error("아티클 동기화 중 오류 발생", e);
		}
	}
	
	/**
	 * DB에서 미동기화 아티클 ID 조회 (최근 24시간)
	 */
	private Set<String> findUnsyncedArticleIdsFromDB() {
		LocalDateTime since = LocalDateTime.now().minusHours(24);
		
		Set<String> articleIds = new HashSet<>();
		
		try {
			// 댓글에서 미동기화 아티클 ID 조회
			List<String> commentArticleIds = userCommentRepository.findUnsyncedArticleIds(since);
			articleIds.addAll(commentArticleIds);
			
			// 좋아요에서 미동기화 아티클 ID 조회
			List<String> likeArticleIds = userLikeRepository.findUnsyncedArticleIds(since);
			articleIds.addAll(likeArticleIds);
			
			if (!articleIds.isEmpty()) {
				log.info("DB에서 미동기화 아티클 {}개 발견", articleIds.size());
			}
		} catch (Exception e) {
			log.error("DB 미동기화 아티클 조회 실패", e);
		}
		
		return articleIds;
	}
	
	/**
	 * 아티클 서버에서 배치 조회 후 동기화
	 */
	private int syncArticles(List<String> articleIds) {
		int totalSynced = 0;
		
		// 100개씩 배치 조회
		for (int i = 0; i < articleIds.size(); i += 100) {
			int end = Math.min(i + 100, articleIds.size());
			List<String> batch = articleIds.subList(i, end);
			
			try {
				// 아티클 서버에서 조회
				List<ArticleDto> articles = articleClient.getArticlesByIds(batch);
				
				if (!articles.isEmpty()) {
					// 트랜잭션 단위로 업데이트
					updateArticleData(articles);
					totalSynced += articles.size();
				}
			} catch (Exception e) {
				log.error("배치 동기화 실패: batch={}", batch, e);
			}
		}
		
		return totalSynced;
	}
	
	/**
	 * 아티클 정보로 UserArticle, UserComment, UserLike 업데이트
	 */
	@Transactional
	protected void updateArticleData(List<ArticleDto> articles) {
		for (ArticleDto dto : articles) {
			try {
				UserArticleKey key = new UserArticleKey(dto.getWriterId(), dto.getArticleId());
				
				// 1. UserArticle 저장/업데이트
				userArticleRepository.findById(key).ifPresentOrElse(
						existing -> {
							existing.setTitle(dto.getTitle());
							existing.setVersion(dto.getVersion().intValue());
							existing.setCreatedAt(dto.getCreatedAt());
						},
						() -> userArticleRepository.save(new UserArticle(
								key, dto.getTitle(), dto.getVersion(), dto.getCreatedAt()
						))
				);
				
				// 2. UserComment 업데이트 (해당 아티클의 모든 댓글)
				List<UserComment> comments = userCommentRepository.findAllByIdArticleId(dto.getArticleId());
				comments.forEach(comment -> {
					comment.setTitle(dto.getTitle());
					comment.setVersion(dto.getVersion().intValue());
					comment.setCreatedAt(dto.getCreatedAt());
					comment.setArticleSynced(true); // 동기화 완료 표시
				});
				
				// 3. UserLike 업데이트 (해당 아티클의 모든 좋아요)
				List<UserLike> likes = userLikeRepository.findAllByIdArticleId(dto.getArticleId());
				likes.forEach(like -> {
					like.setTitle(dto.getTitle());
					like.setVersion(dto.getVersion().intValue());
					like.setCreatedAt(dto.getCreatedAt());
					like.setArticleSynced(true); // 동기화 완료 표시
				});
				
				log.debug("아티클 동기화 완료: articleId={}, comments={}, likes={}",
						dto.getArticleId(), comments.size(), likes.size());
				
			} catch (Exception e) {
				log.error("아티클 업데이트 실패: articleId={}", dto.getArticleId(), e);
			}
		}
	}
}
