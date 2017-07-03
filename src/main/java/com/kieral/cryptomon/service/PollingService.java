package com.kieral.cryptomon.service;

public interface PollingService {

	/**
	 * Registers a listener for polling callbacks - must be a unique name
	 */
	void registerListener(String name, PollListener listener);
	
}
