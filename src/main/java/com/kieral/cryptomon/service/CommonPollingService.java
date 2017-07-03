package com.kieral.cryptomon.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CommonPollingService implements PollingService {

	private final ScheduledExecutorService commonScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(r, "ExchangeServiceScheduler");
		thread.setDaemon(true);
		return thread;
	}});
	private final ConcurrentMap<String, PollClient> pollClients = new ConcurrentHashMap<String, PollClient>();
			
	@Autowired
	CommonConfig commonConfig;
	
	@PostConstruct
	public void init() {
		commonScheduler.scheduleAtFixedRate(() -> {callPollers();}, commonConfig.pollingInterval, commonConfig.pollingInterval, TimeUnit.MILLISECONDS);
	}

	@Override
	public void registerListener(String name, PollListener listener) {
		if (listener == null || name == null)
			return;
		if (!pollClients.containsKey(name))
			pollClients.putIfAbsent(name, new PollClient(listener, createExecutorService(name)));
	}

	private void callPollers() {
		pollClients.values().forEach(pollClient -> {
			try {
				pollClient.processor.submit(() -> {pollClient.listener.polled();});;
			} catch (Exception e) {
				pollClient.listener.polledException(e);
			}
		});
	}
	
	private ExecutorService createExecutorService(final String name) {
		return Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, name + "-ScheduleProcessor");
				thread.setDaemon(true);
				return thread;
			}});
	}
	
	class PollClient {
		PollListener listener;
		ExecutorService processor;
		public PollClient(PollListener listener, ExecutorService processor) {
			this.listener = listener;
			this.processor = processor;
		}
	}

}
