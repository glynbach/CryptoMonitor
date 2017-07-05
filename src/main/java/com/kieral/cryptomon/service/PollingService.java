package com.kieral.cryptomon.service;

public interface PollingService {

	void registerListener(String name, PollListener listener);
	
}
