package com.teambind.activityserver.domain.board.repository;

import com.teambind.activityserver.domain.board.entity.UserComment;
import com.teambind.activityserver.domain.board.entity.key.UserArticleKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCommentRepository extends JpaRepository<UserComment, UserArticleKey> {
}
