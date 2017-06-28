package com.kieral.cryptomon.service.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
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

	public List<String> getEnabledExchangeNames() {
		List<ExchangeService> enabledExchanges = getEnabledExchanges();
		if (enabledExchanges == null || enabledExchanges.size() == 0)
			return Collections.emptyList();
		return enabledExchanges.stream().map(ExchangeService::getName).collect(Collectors.<String>toList());
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
	
	public List<String> enableTradingAll(String secretKey) {
		List<String> errors = new ArrayList<String>();
		enabledExchanges.forEach(exchange -> {
			if (!enableTrading(exchange.getName(), secretKey))
				errors.add(String.format("Failed to enable trading for %s", exchange.getName()));
		});
		return errors;
	}
	
	public boolean enableTrading(String market, String secretKey) {
		if (enabledExchangeMap.containsKey(market)) {
			 return enabledExchangeMap.get(market).enableTrading(secretKey);
		} else {
			return false;
		}
	}

	public boolean isTradingEnabled(String market) {
		return !enabledExchangeMap.containsKey(market)
				|| enabledExchangeMap.get(market).isTradingEnabled();
	}

	public void updatesAllBalances(boolean overrideWorkingBalance) {
		enabledExchanges.forEach(exchange -> {
			try {
				exchange.updateBalances(overrideWorkingBalance);
			} catch (BalanceRequestException e) {
				logger.error("Error requesting balance for exchange {}", exchange.getName(), e);
			}
		});
	}

	public void updateBalances(String market, boolean overrideWorkingBalance) {
		if (enabledExchangeMap.containsKey(market)) {
			try {
				enabledExchangeMap.get(market).updateBalances(overrideWorkingBalance);
			} catch (BalanceRequestException e) {
				logger.error("Error requesting balance for exchange {}", market, e);
			}
		} 
	}

	public CurrencyPair getCurrencyPair(String market, String currencyPairStr) {
		if (enabledExchangeMap.containsKey(market)) {
			return enabledExchangeMap.get(market).getCurrencyPair(currencyPairStr);
		}
		return null;
	}

	public OrderStatus placeOrder(Order order) {
		if (checkWithMessageUpdateMarketStatus(order)) {
			return enabledExchangeMap.get(order.getMarket()).placeOrder(order);
		} else {
			return OrderStatus.CANCELLED;
		}
	}

	public OrderStatus cancelOrder(Order order) {
		if (checkWithMessageUpdateMarketStatus(order)) {
			return enabledExchangeMap.get(order.getMarket()).cancelOrder(order);
		} else {
			return order.getOrderStatus();
		}
	}

	public OrderStatus forceCancelOrder(String market, String orderId) {
		if (enabledExchangeMap.containsKey(market))
			return enabledExchangeMap.get(market).cancelOrder(orderId);
		return OrderStatus.ERROR;
	}
	
	public Map<String, OrderStatus> getOpenOrderStatuses(String market, List<Order> orders) {
		if (checkMarketStatus(market)) {
			return enabledExchangeMap.get(market).getOpenOrderStatuses(orders);
		} else {
			return null;
		}
	}
	public List<Order> getOpenOrders(String market) {
		if (checkMarketStatus(market, true)) {
			return enabledExchangeMap.get(market).getOpenOrders();
		} else {
			return null;
		}
	}

	private boolean checkWithMessageUpdateMarketStatus(Order order) {
		if (!enabledExchangeMap.containsKey(order.getMarket())) {
			order.setMessage(String.format("Exchange %s not enabled", order.getMarket()));
			return false;
		}
		if (getExchangeStatus(order.getMarket()) != ConnectionStatus.CONNECTED) {
			order.setMessage(String.format("Exchange %s not connected", order.getMarket()));
			return false;
		}
		if (!isTradingEnabled(order.getMarket())) {
			order.setMessage(String.format("Trading not enabled for Exchange %s", order.getMarket()));
			return false;
		}
		return true;
	}

	private boolean checkMarketStatus(String market) {
		return checkMarketStatus(market, false);
	}

	private boolean checkMarketStatus(String market, boolean throwException) {
		if (!enabledExchangeMap.containsKey(market)) {
			if (throwException)
				throw new IllegalStateException(String.format("%s is not enabled", market));
			return false;
		}
		if (getExchangeStatus(market) != ConnectionStatus.CONNECTED) {
			if (throwException)
				throw new IllegalStateException(String.format("%s is not connected", market));
			return false;
		}
		if (!isTradingEnabled(market)) {
			if (throwException)
				throw new IllegalStateException(String.format("%s is not trading enabled", market));
			return false;
		}
		return true;
	}

	public ServiceExchangeProperties getProperties(String market) {
		return enabledExchangeMap.containsKey(market) ? enabledExchangeMap.get(market).getProperties() : null;
	}

}
