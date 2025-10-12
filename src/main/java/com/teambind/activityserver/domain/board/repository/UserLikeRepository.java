package com.teambind.activityserver.domain.board.repository;

import com.teambind.activityserver.domain.board.entity.UserLike;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserLikeRepository extends JpaRepository<UserLike, UserArticleKey> {
}
