package com.kieral.cryptomon.model.sided;

import java.util.ArrayList;
import java.util.List;

import com.kieral.cryptomon.model.general.Side;

public abstract class SidedValue<T> {

	private final T bidValue;
	private final T askValue;
	
	public SidedValue(T bidValue, T askValue) {
		this.bidValue = bidValue;
		this.askValue = askValue;
	}
	
	protected T getBidValue() {
		return bidValue;
	}
	
	protected T getAskValue() {
		return askValue;
	}
	
	public T get(Side side) {
		if (side == null)
			throw new IllegalArgumentException("side can not be null");
		return side == Side.BID ? getBidValue() : getAskValue();
	}
	
	public List<T> values() {
		List<T> rtn = new ArrayList<T>();
		return rtn;
	}
	
}
