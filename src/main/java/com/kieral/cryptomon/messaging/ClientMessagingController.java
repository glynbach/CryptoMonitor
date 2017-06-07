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

import com.kieral.cryptomon.service.liquidity.BaseService;

@Controller
@EnableScheduling
public class ClientMessagingController {

	private static final Comparator<OrderBookMessage> obComparator = new Comparator<OrderBookMessage>(){
		@Override
		public int compare(OrderBookMessage o1, OrderBookMessage o2) {
			int marketCompare = o1.getMarket().compareTo(o2.getCurrencyPair()); 
			if (marketCompare == 0)
				return o1.getCurrencyPair().compareTo(o2.getCurrencyPair());
			return marketCompare;
		}
	}; 

	@Autowired 
	private BaseService poloniexService;
	@Autowired 
	private BaseService bittrexService;
	private ConcurrentMap<String, ConcurrentMap<String, OrderBookMessage>> orderBooks = new ConcurrentHashMap<String, ConcurrentMap<String, OrderBookMessage>>();
	private List<String> subscriptions = Collections.synchronizedList(new ArrayList<String>());

    @Autowired
    private SimpMessagingTemplate template;
    
    @PostConstruct
    public void init(){
		poloniexService.registerOrderBookListener(orderBook -> {
			orderBooks.putIfAbsent(poloniexService.getName(), new ConcurrentHashMap<String, OrderBookMessage>());
			orderBooks.get(poloniexService.getName()).put(orderBook.getCurrencyPair().getName(), new OrderBookMessage(orderBook));
		});
		bittrexService.registerOrderBookListener(orderBook -> {
			orderBooks.putIfAbsent(bittrexService.getName(), new ConcurrentHashMap<String, OrderBookMessage>());
			orderBooks.get(bittrexService.getName()).put(orderBook.getCurrencyPair().getName(), new OrderBookMessage(orderBook));
		});
    }

    @MessageMapping("/orderBook")
    @SendTo("/topic/orderBooks")
    public List<OrderBookMessage> subscribeOrderBooks(SubscriptionMessage subscription) {
    	if (!subscriptions.contains(subscription.getMarket()))
    		subscriptions.add(subscription.getMarket());
        return getOrderBooksForMarket(subscription.getMarket());
    }

    @Scheduled(fixedRate = 2000)
    public void publishUpdates(){
		subscriptions.forEach(market -> {
			template.convertAndSend("/topic/orderBooks", getOrderBooksForMarket(market));
		});
    }

    private List<OrderBookMessage> getOrderBooksForMarket(String market) {
    	List<OrderBookMessage> rtn = new ArrayList<OrderBookMessage>(); 
    	if ("ALL".equalsIgnoreCase(market)) {
    		orderBooks.values().forEach(bookMap -> {
    			rtn.addAll(bookMap.values());
    		});
    	} else if (orderBooks.containsKey(market)) {
			rtn.addAll(orderBooks.get(market).values());
    	}
    	if (rtn.size() > 0) {
    		rtn.sort(obComparator);
    	}
    	return rtn;
    }
}
