package com.kieral.cryptomon.service.exchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.connection.ConnectionStatusListener;
import com.kieral.cryptomon.service.exception.BalanceRequestException;
import com.kieral.cryptomon.service.liquidity.OrderBookListener;

public class ExchangeManagerService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final List<ExchangeService> enabledExchanges = new ArrayList<ExchangeService>();
	private final Map<String, ExchangeService> enabledExchangeMap = new HashMap<String, ExchangeService>();
	private final ConcurrentMap<String, ConnectionStatus> exchangeStatuses = new ConcurrentHashMap<String, ConnectionStatus>();
	
	public ExchangeManagerService(ExchangeService... services) {
		if (services != null) {
			for (ExchangeService service: services) {
				if (service.isEnabled()) {
					enabledExchanges.add(service);
					enabledExchangeMap.put(service.getName(), service);
					service.registerStatusListener(status -> {
						exchangeStatuses.putIfAbsent(service.getName(), status);
					});
				}
			}
		}
	}

	public List<ExchangeService> getEnabledExchanges() {
		return new ArrayList<ExchangeService>(enabledExchanges);
	}

	public void registerAllOrderBookListeners(OrderBookListener listener) {
		if (listener == null)
			return;
		enabledExchanges.forEach(exchange -> {
			exchange.registerOrderBookListener(listener);
		});
	}
	
	public ConnectionStatus registerConnectionStatusListener(String market, ConnectionStatusListener listener) {
		if (market == null || listener == null)
			return ConnectionStatus.DISCONNECTED;
		AtomicReference<ConnectionStatus> rtnStatus = new AtomicReference<ConnectionStatus>(ConnectionStatus.DISCONNECTED); 
		enabledExchanges.forEach(exchange -> {
			if (market.equals(exchange.getName())) {
				exchange.registerStatusListener(listener);
				rtnStatus.set(exchangeStatuses.getOrDefault(market, ConnectionStatus.DISCONNECTED));
			}
		});
		return rtnStatus.get();
	}
	
	public ConnectionStatus getExchangeStatus(String market) {
		return exchangeStatuses.getOrDefault(market, ConnectionStatus.DISCONNECTED);
	}
	
	public List<String> unlockTradingAll(String secretKey) {
		List<String> errors = new ArrayList<String>();
		enabledExchanges.forEach(exchange -> {
			String error = unlockTrading(exchange.getName(), secretKey);
			if (error != null)
				errors.add(error);
		});
		return errors;
	}
	
	public String unlockTrading(String market, String secretKey) {
		if (enabledExchangeMap.containsKey(market)) {
			if (!enabledExchangeMap.get(market).unlockTrading(secretKey)) {
				return updateBalances(market, true); 
			} else {
				return String.format("Exchange %s still locked", market); 
			}
		} else {
			return String.format("Exchange %s is not enabled or does not exist", market);
		}
	}
	
	public List<String> updatesAllBalances(boolean overrideWorkingBalance) {
		List<String> errors = new ArrayList<String>();
		enabledExchanges.forEach(exchange -> {
			try {
				exchange.updateBalances(overrideWorkingBalance);
			} catch (Exception e) {
				logger.error("Error requesting balance for exchange {}", exchange.getName(), e);
				errors.add(e.getMessage());
			}
		});
		return errors;
	}

	public String updateBalances(String market, boolean overrideWorkingBalance) {
		if (enabledExchangeMap.containsKey(market)) {
			try {
				enabledExchangeMap.get(market).updateBalances(overrideWorkingBalance);
			} catch (BalanceRequestException e) {
				logger.error("Error requesting balance for exchange {}", market, e);
				return e.getMessage();
			}
		} else
			return String.format("Exchange %s is not enabled or does not exist", market);
		return null;
	}

}
