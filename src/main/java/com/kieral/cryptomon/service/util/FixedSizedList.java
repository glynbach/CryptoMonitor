package com.kieral.cryptomon.service.util;

import java.util.Collection;
import java.util.LinkedList;

public class FixedSizedList<E> extends LinkedList<E> {

	private static final long serialVersionUID = 1564347158434041086L;
	
	private final int maxSize;
	
	public FixedSizedList(int maxSize) {
		super();
		this.maxSize = maxSize;
	}

	@Override
	public boolean add(E element) {
		boolean rtn = super.add(element);
		if (this.size() > maxSize)
			this.remove(0);
		return rtn;
	}

	@Override
	public boolean addAll(Collection<? extends E> elements) {
		boolean rtn = super.addAll(elements);
		if (this.size() > maxSize)
			this.remove(0);
		return rtn;
	}

}