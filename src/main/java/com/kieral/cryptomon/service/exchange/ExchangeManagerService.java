package com.kieral.cryptomon.service.exchange;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.connection.IStatusListener;
import com.kieral.cryptomon.service.liquidity.IOrderBookListener;

public class ExchangeManagerService {

	private final List<IExchangeService> enabledExchanges = new ArrayList<IExchangeService>();
	private final ConcurrentMap<String, ConnectionStatus> exchangeStatuses = new ConcurrentHashMap<String, ConnectionStatus>();
	
	public ExchangeManagerService(IExchangeService... services) {
		if (services != null) {
			for (IExchangeService service: services) {
				if (service.isEnabled()) {
					enabledExchanges.add(service);
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
	
}
