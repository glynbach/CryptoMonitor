package com.kieral.cryptomon.tickstore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.model.orderbook.OrderBook;

public class DaoManager implements IOrderBookDao {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	IOrderBookDao orderBookDao;
	
	private final ExecutorService processor = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "tickstoreProcessor");
			thread.setDaemon(true);
			return thread;
		}
	});
	
	@Override
	public void saveOrderBookDataEntries(final OrderBook orderBook) {
		processor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					orderBookDao.saveOrderBookDataEntries(orderBook);
				} catch (Exception e) {
					logger.error("Error saving {} to tickstore", orderBook, e);
				}
			}
		});
	}

}
