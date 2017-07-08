package com.kieral.cryptomon.service.exchange.yobit.payload;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class YobitResponseContainer<T> {

	protected boolean success;
	protected T returnValue;
	
	public boolean isSuccess() {
		return success;
	}
	
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	public T getReturnValue() {
		return returnValue;
	}
	
	@JsonProperty("return")
	public void setReturnValue(T returnValue) {
		this.returnValue = returnValue;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " [success=" + success + ", returnValue=" + returnValue + "]";
	}
	
}
