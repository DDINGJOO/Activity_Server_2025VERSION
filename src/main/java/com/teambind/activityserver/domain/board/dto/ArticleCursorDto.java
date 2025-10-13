package com.teambind.activityserver.domain.board.dto;

import java.time.LocalDateTime;

/**
 * 도메인 레벨 DTO: articleId와 createdAt을 함께 전달하기 위한 객체
 * articleId는 문자열 타입(String)으로 처리합니다.
 */
public record ArticleCursorDto(String articleId, LocalDateTime createdAt) {

}
