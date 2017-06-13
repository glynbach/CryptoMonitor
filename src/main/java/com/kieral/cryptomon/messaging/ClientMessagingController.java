package com.kieral.cryptomon.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.kieral.cryptomon.model.accounting.Balance;
import com.kieral.cryptomon.service.BalanceHandler;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.exchange.IExchangeService;

@Controller
@EnableScheduling
public class ClientMessagingController {

	private static final Comparator<OrderBookMessage> obComparator = new Comparator<OrderBookMessage>(){
		@Override
		public int compare(OrderBookMessage o1, OrderBookMessage o2) {
			int ccyPairCompare = o1.getCurrencyPair().compareTo(o2.getCurrencyPair()); 
			if (ccyPairCompare == 0)
				return o1.getMarket().compareTo(o2.getMarket());
			return ccyPairCompare;
		}
	}; 

	private static final Comparator<BalanceEntryMessage> balanceComparator = new Comparator<BalanceEntryMessage>(){
		@Override
		public int compare(BalanceEntryMessage o1, BalanceEntryMessage o2) {
			return o1.getCurrency().compareTo(o2.getCurrency());
		}
	}; 

	@Autowired 
	ExchangeManagerService exchangeManager;
	@Autowired
	BalanceHandler balanceHandler;

	private List<IExchangeService> exchanges = new ArrayList<IExchangeService>();
	private ConcurrentMap<String, ConcurrentMap<String, OrderBookMessage>> orderBooks = new ConcurrentHashMap<String, ConcurrentMap<String, OrderBookMessage>>();
	private ConcurrentMap<String, ConnectionStatus> statuses = new ConcurrentHashMap<String, ConnectionStatus>();
	private List<String> subscriptions = Collections.synchronizedList(new ArrayList<String>());

    @Autowired
    private SimpMessagingTemplate template;
    
    @PostConstruct
    public void init(){
    	exchangeManager.getEnabledExchanges().forEach(service -> {
    		exchanges.add(service);
    		service.registerOrderBookListener(orderBook -> {
    			orderBooks.putIfAbsent(service.getName(), new ConcurrentHashMap<String, OrderBookMessage>());
    			orderBooks.get(service.getName()).put(orderBook.getCurrencyPair().getName(), new OrderBookMessage(orderBook));
    		});
    		service.registerStatusListener(status  -> {
    			ConnectionStatus lastStatus;
    			lastStatus = statuses.put(service.getName(), status);
    			if (lastStatus == null || lastStatus != status)
    				template.convertAndSend("/topic/exchangeStatus", getExchangeStatus(service.getName()));
    		});
    	});
    }

    @MessageMapping("/exchangeStatus")
    @SendTo("/topic/exchangeStatus")
    public List<ExchangeStatusMessage> subscribeOrderBooks(SubscriptionMessage subscription) {
    	if (!subscriptions.contains(subscription.getMarket()))
    		subscriptions.add(subscription.getMarket());
        return getExchangeStatus(subscription.getMarket());
    }

    @Scheduled(fixedRate = 2000)
    public void publishUpdates(){
		subscriptions.forEach(market -> {
			template.convertAndSend("/topic/exchangeStatus", getExchangeStatus(market));
		});
    }

    private List<ExchangeStatusMessage> getExchangeStatus(String market) {
    	List<ExchangeStatusMessage> rtn = new ArrayList<ExchangeStatusMessage>();
    	exchanges.forEach(exchange -> {
    		if ("ALL".equalsIgnoreCase(market) || market.equalsIgnoreCase(exchange.getName())) {
    			String name = exchange.getName();
    			List<OrderBookMessage> orderBookMessages = new ArrayList<OrderBookMessage>();
    			if (orderBooks.containsKey(name))
    				orderBookMessages.addAll(orderBooks.get(name).values());
    			orderBookMessages.sort(obComparator);
    			boolean connected = statuses.get(name) == ConnectionStatus.CONNECTED;
    			boolean tradingLocked = exchange.isTradingLocked();
    			BalanceMessage balances = balanceMessageFor(name, balanceHandler.getBalances(name));
    			rtn.add(new ExchangeStatusMessage(name, connected, tradingLocked, balances, orderBookMessages));
    		}
    	});
    	return rtn;
    }
    
    private BalanceMessage balanceMessageFor(String market, List<Balance> balances) {
    	List<BalanceEntryMessage> entries = new ArrayList<BalanceEntryMessage>();
    	if (balances != null) {
    		balances.forEach(balance -> {
    			entries.add(new BalanceEntryMessage(balance.getCurrency().name(),
    					balance.getWorkingAmount().toPlainString()));
    		});
    		entries.sort(balanceComparator);
    	}
    	return new BalanceMessage(market, entries);
    }
}
