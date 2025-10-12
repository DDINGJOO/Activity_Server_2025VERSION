package com.teambind.activityserver.domain.board.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_board_activities_count")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserBoardActivitiesCount {
	@Id
	@Column(name = "user_id")
	private String userId;
	
	@Column(name = "article_count")
	private Integer totalArticleCount;
	
	@Column(name = "commented_article_count")
	private Integer totalCommentCount;
	
	@Column(name = "liked_article_count")
	private Integer totalLikeCount;
	
	public UserBoardActivitiesCount(String userId) {
		this.userId = userId;
		this.totalArticleCount = 0;
		this.totalCommentCount = 0;
		this.totalLikeCount = 0;
	}
	
	
	// 편의성 메서드
	public void increaseArticleCount() {
		this.totalArticleCount++;
	}
	
	public void increaseCommentCount() {
		this.totalCommentCount++;
	}
	
	public void increaseLikeCount() {
		this.totalLikeCount++;
	}
	
	public void decreaseArticleCount() {
		this.totalArticleCount--;
	}
	
	public void decreaseCommentCount() {
		this.totalCommentCount--;
	}
	
	public void decreaseLikeCount() {
		this.totalLikeCount--;
	}
	
}
