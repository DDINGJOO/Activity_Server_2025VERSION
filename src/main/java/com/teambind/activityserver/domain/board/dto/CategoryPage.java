package com.teambind.activityserver.domain.board.dto;

import java.util.List;

/**
 * 도메인 레벨의 카테고리 페이징 결과 DTO
 * articleId는 문자열(String)로 전달됩니다.
 */
public record CategoryPage(List<String> articleIds, String nextCursor) {


}
