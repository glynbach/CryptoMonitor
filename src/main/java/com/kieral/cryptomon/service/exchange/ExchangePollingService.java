package com.kieral.cryptomon.service.exchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.service.CommonConfig;
import com.kieral.cryptomon.service.PollListener;
import com.kieral.cryptomon.service.PollingService;

public class ExchangePollingService implements PollingService {

	private ScheduledFuture<?> pollingFuture;
	
	private final ScheduledExecutorService commonScheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
	@Override
	public Thread newThread(Runnable r) {
		Thread thread = new Thread(r, "ExchangeServiceScheduler");
		thread.setDaemon(true);
		return thread;
	}});
	
	// Insertion order is important here - typically the snapshot pollers will be added before
	// the execution monitors
	private final Map<String, PollClient> pollClients = new LinkedHashMap<String, PollClient>();
	private final Object clientLock = new Object();
			
	@Autowired
	CommonConfig commonConfig;
	
    private final AtomicBoolean initialised = new AtomicBoolean(false);
	
	@PostConstruct
	public void init() {
		if (initialised.compareAndSet(false, true))
			schedulePolling(commonConfig.getPollingInterval());
	}

	@Override
	public void registerListener(String name, PollListener listener) {
		if (listener == null || name == null)
			return;
		synchronized(clientLock) {
			if (!pollClients.containsKey(name))
				pollClients.put(name, new PollClient(listener, createExecutorService(name)));
		}
	}

	@Override
	public void unRegisterListener(String name) {
		synchronized(clientLock) {
			pollClients.remove(name);
		}
	}

	public void changePollingInterval(long interval) {
		schedulePolling(interval);
	}

	public void restorePollingInterval() {
		schedulePolling(commonConfig.getPollingInterval());
	}

	private void schedulePolling(long interval) {
		if (pollingFuture != null && !pollingFuture.isDone()) { 
			try {
				pollingFuture.cancel(false);
			} catch (Exception e) {
			}
		}
		pollingFuture = commonScheduler.scheduleAtFixedRate(() -> {callPollers();}, interval, interval, TimeUnit.MILLISECONDS);
	}
	
	private void callPollers() {
		List<PollClient> pollClients;
		synchronized(clientLock) {
			pollClients = new ArrayList<PollClient>(this.pollClients.values());
		}
		pollClients.forEach(pollClient -> {
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
