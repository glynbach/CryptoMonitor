package com.kieral.cryptomon.service.exchange.bittrex.payload;

public abstract class BittrexResponseContainer<T> {

	protected boolean success;
	protected String message;
	protected T result;
	
	public boolean isSuccess() {
		return success;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public T getResult() {
		return result;
	}
	
	public void setResult(T result) {
		this.result = result;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " [success=" + success + ", message=" + message + ", result=" + result + "]";
	}
	
}
