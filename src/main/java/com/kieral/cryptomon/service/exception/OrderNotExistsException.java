package com.kieral.cryptomon.service.exception;

public class OrderNotExistsException extends Exception {

	private static final long serialVersionUID = -7203336137208223320L;

	public OrderNotExistsException() {
		super();
	}

	public OrderNotExistsException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public OrderNotExistsException(String message, Throwable cause) {
		super(message, cause);
	}

	public OrderNotExistsException(String message) {
		super(message);
	}

	public OrderNotExistsException(Throwable cause) {
		super(cause);
	}

}
