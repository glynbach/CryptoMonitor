package com.kieral.cryptomon.service.exception;

public class ApiRequestException extends Exception {

	private static final long serialVersionUID = 7610396613803226137L;

	public ApiRequestException() {
		super();
	}

	public ApiRequestException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ApiRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public ApiRequestException(String message) {
		super(message);
	}

	public ApiRequestException(Throwable cause) {
		super(cause);
	}

}
