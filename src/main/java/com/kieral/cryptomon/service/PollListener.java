package com.kieral.cryptomon.service;

public interface PollListener {

	/**
	 * Calls on scheduler notifying readiness to poll
	 */
	void polled();

	/**
	 * Calls if scheduler detected an exception in polling
	 */
	void polledException(Exception e);
	
}
