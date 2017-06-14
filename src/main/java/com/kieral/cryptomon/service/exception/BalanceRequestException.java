package com.kieral.cryptomon.service.exception;

public class BalanceRequestException extends Exception {

	private static final long serialVersionUID = 2327172534664399183L;

	public BalanceRequestException() {
		super();
	}

	public BalanceRequestException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BalanceRequestException(String message, Throwable cause) {
		super(message, cause);
	}

	public BalanceRequestException(String message) {
		super(message);
	}

	public BalanceRequestException(Throwable cause) {
		super(cause);
	}

}
