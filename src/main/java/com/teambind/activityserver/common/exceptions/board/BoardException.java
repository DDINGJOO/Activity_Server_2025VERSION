package com.teambind.activityserver.common.exceptions.board;

import org.springframework.http.HttpStatus;


public class BoardException extends RuntimeException {
	private final ErrorCode errorcode;
	
	public BoardException(ErrorCode errorcode) {
		
		super(errorcode.toString());
		this.errorcode = errorcode;
	}
	
	public HttpStatus getStatus() {
		return errorcode.getStatus();
	}
	
	public ErrorCode getErrorCode() {
		return errorcode;
	}
}
