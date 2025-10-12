package com.teambind.activityserver.domain.board.entity.key;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserArticleKey implements Serializable {
	
	private String userId;
	private String articleId;
}
