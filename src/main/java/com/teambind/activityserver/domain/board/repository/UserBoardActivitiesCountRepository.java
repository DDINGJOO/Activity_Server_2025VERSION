package com.teambind.activityserver.domain.board.repository;

import com.teambind.activityserver.domain.board.entity.UserBoardActivitiesCount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBoardActivitiesCountRepository extends JpaRepository<UserBoardActivitiesCount, String> {
}
