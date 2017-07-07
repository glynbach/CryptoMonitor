package com.kieral.cryptomon.service;

public interface PollingService {

	void registerListener(String name, PollListener listener);

	void unRegisterListener(String name);

	void changePollingInterval(long interval);

	void restorePollingInterval();

}
