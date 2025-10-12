package com.teambind.activityserver.common.exceptions.board;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
	
	;
	private final String errCode;
	private final String message;
	private final HttpStatus status;
	
	ErrorCode(String errCode, String message, HttpStatus status) {
		
		this.status = status;
		this.errCode = errCode;
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "ErrorCode{"
				+ " status='"
				+ status
				+ '\''
				+ "errCode='"
				+ errCode
				+ '\''
				+ ", message='"
				+ message
				+ '\''
				+ '}';
	}
}

