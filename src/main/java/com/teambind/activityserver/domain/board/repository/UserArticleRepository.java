package com.teambind.activityserver.domain.board.repository;

import com.teambind.activityserver.domain.board.entity.UserArticle;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserArticleRepository extends JpaRepository<UserArticle, UserArticleKey> {
}
