package com.kieral.cryptomon.service.exception;

public class BalanceUnavailableException extends Exception {

	private static final long serialVersionUID = 2327172534664399183L;

	public BalanceUnavailableException() {
		super();
	}

	public BalanceUnavailableException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BalanceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public BalanceUnavailableException(String message) {
		super(message);
	}

	public BalanceUnavailableException(Throwable cause) {
		super(cause);
	}

}
