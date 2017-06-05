package com.kieral.cryptomon.messaging;

import java.util.ArrayList;
import java.util.Collections;
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

import com.kieral.cryptomon.service.liquidity.AbstractLiquidityProvider;

@Controller
@EnableScheduling
public class ClientMessagingController {

	@Autowired 
	private AbstractLiquidityProvider poloniexService;
	private ConcurrentMap<String, OrderBookMessage> orderBooks = new ConcurrentHashMap<String, OrderBookMessage>();
	private List<String> subscriptions = Collections.synchronizedList(new ArrayList<String>());

    @Autowired
    private SimpMessagingTemplate template;
    
    @PostConstruct
    public void init(){
    	try {
    		poloniexService.registerOrderBookListener(orderBook -> {
    			orderBooks.put(poloniexService.getName(), new OrderBookMessage(orderBook));
    		});
			poloniexService.connect();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    @MessageMapping("/orderBook")
    @SendTo("/topic/orderBooks")
    public OrderBookMessage subscribeOrderBooks(SubscriptionMessage subscription) {
    	if (!subscriptions.contains(subscription.getMarket()))
    		subscriptions.add(subscription.getMarket());	
    	if (orderBooks.containsKey(subscription.getMarket()))
			return orderBooks.get(subscription.getMarket());
        return new OrderBookMessage();
    }

    @Scheduled(fixedRate = 500)
    public void publishUpdates(){
       subscriptions.forEach(market -> {
           template.convertAndSend("/topic/orderBooks", orderBooks.getOrDefault(market, new OrderBookMessage()));
       });
    }

}
