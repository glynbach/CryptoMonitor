package com.kieral.cryptomon.streaming;

@SuppressWarnings("serial")
public class ParsingPayloadException extends Exception {

	public ParsingPayloadException() {
		super();
	}

	public ParsingPayloadException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public ParsingPayloadException(String message, Throwable cause) {
		super(message, cause);
	}

	public ParsingPayloadException(String message) {
		super(message);
	}

	public ParsingPayloadException(Throwable cause) {
		super(cause);
	}

}
