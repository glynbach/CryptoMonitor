package com.kieral.cryptomon.service.util.threading;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple set of single threaded executors that are bound to the same key
 */
public class StickyThreadPool {

	private final static int MAX_POOL_SIZE = 20;
	
	private final String poolName;
	private final int poolSize;
	private final AtomicInteger lastKeyAssigned = new AtomicInteger(0);
	private int lastKeyUsed = 0;
	
	private final ConcurrentMap<String, Integer> keyMap = new ConcurrentHashMap<String, Integer>();
	private final ConcurrentMap<Integer, ExecutorService> threadPool 
						= new ConcurrentHashMap<Integer, ExecutorService>();
	
	public StickyThreadPool(String poolName, int poolSize) {
		if (poolSize <= 0 || poolSize > MAX_POOL_SIZE)
			throw new IllegalArgumentException(String.format("poolSize must be between 1 and %s", MAX_POOL_SIZE));
		this.poolName = poolName;
		this.poolSize = poolSize;
	}
	
	public ExecutorService getSingleThreadExecutor(String key) {
		int poolKey;
		if (!keyMap.containsKey(key)) {
			// New or assign
			while (true) {
				int lastKey = lastKeyAssigned.get();
				if (lastKey < poolSize) {
					if (lastKeyAssigned.compareAndSet(lastKey, lastKey+1)) {
						threadPool.putIfAbsent(lastKey, Executors.newSingleThreadExecutor(new ThreadFactory() {
							@Override
							public Thread newThread(Runnable runnable) {
								Thread thread = new Thread(runnable, poolName + "-" + (lastKey));
								thread.setDaemon(true);
								return thread;
							}
						}));
						break;
					}
				} else
					break;
			}
			// No need to be thread safe about which key to assign, doesn't matter if its not 
			// totally fairly distributed
			// Note: keys will be in the map from 0 -> poolSize - 1
			poolKey = lastKeyUsed++;
			if (lastKeyUsed >= poolSize) {
				lastKeyUsed =0;
			}
			if (!(keyMap.putIfAbsent(key, poolKey) == null))
				poolKey = keyMap.get(key);
		} else 
			poolKey = keyMap.get(key);
		return threadPool.get(poolKey);
	}
	
}
