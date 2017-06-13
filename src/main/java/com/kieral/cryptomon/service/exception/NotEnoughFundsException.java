package com.kieral.cryptomon.service.exception;

public class NotEnoughFundsException extends Exception {

	private static final long serialVersionUID = 2327172534664399183L;

	public NotEnoughFundsException() {
		super();
	}

	public NotEnoughFundsException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotEnoughFundsException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotEnoughFundsException(String message) {
		super(message);
	}

	public NotEnoughFundsException(Throwable cause) {
		super(cause);
	}

}
