package com.kieral.cryptomon.service.exception;

public class SecurityModuleException extends Exception {

	private static final long serialVersionUID = -8198837553369093142L;

	public SecurityModuleException() {
		super();
	}

	public SecurityModuleException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SecurityModuleException(String message, Throwable cause) {
		super(message, cause);
	}

	public SecurityModuleException(String message) {
		super(message);
	}

	public SecurityModuleException(Throwable cause) {
		super(cause);
	}

}
