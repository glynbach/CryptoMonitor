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
import com.kieral.cryptomon.service.connection.IStatusListener;
import com.kieral.cryptomon.service.exception.BalanceRequestException;
import com.kieral.cryptomon.service.liquidity.IOrderBookListener;

public class ExchangeManagerService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private final List<IExchangeService> enabledExchanges = new ArrayList<IExchangeService>();
	private final Map<String, IExchangeService> enabledExchangeMap = new HashMap<String, IExchangeService>();
	private final ConcurrentMap<String, ConnectionStatus> exchangeStatuses = new ConcurrentHashMap<String, ConnectionStatus>();
	
	public ExchangeManagerService(IExchangeService... services) {
		if (services != null) {
			for (IExchangeService service: services) {
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

	public List<IExchangeService> getEnabledExchanges() {
		return new ArrayList<IExchangeService>(enabledExchanges);
	}

	public void registerAllOrderBookListeners(IOrderBookListener listener) {
		if (listener == null)
			return;
		enabledExchanges.forEach(exchange -> {
			exchange.registerOrderBookListener(listener);
		});
	}
	
	public ConnectionStatus registerConnectionStatusListener(String market, IStatusListener listener) {
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
				return requestBalance(market, true); 
			} else {
				return String.format("Exchange %s still locked", market); 
			}
		} else {
			return String.format("Exchange %s is not enabled or does not exist", market);
		}
	}
	
	public List<String> requestAllBalances(boolean overrideWorkingBalance) {
		List<String> errors = new ArrayList<String>();
		enabledExchanges.forEach(exchange -> {
			try {
				exchange.requestBalance(overrideWorkingBalance);
			} catch (Exception e) {
				logger.error("Error requesting balance for exchange {}", exchange.getName(), e);
				errors.add(e.getMessage());
			}
		});
		return errors;
	}

	public String requestBalance(String market, boolean overrideWorkingBalance) {
		if (enabledExchangeMap.containsKey(market)) {
			try {
				enabledExchangeMap.get(market).requestBalance(overrideWorkingBalance);
			} catch (BalanceRequestException e) {
				logger.error("Error requesting balance for exchange {}", market, e);
				return e.getMessage();
			}
		} else
			return String.format("Exchange %s is not enabled or does not exist", market);
		return null;
	}

}
