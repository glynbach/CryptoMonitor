package com.kieral.cryptomon.messaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import com.kieral.cryptomon.messaging.model.ArbStatusMessage;
import com.kieral.cryptomon.messaging.model.BalanceEntryMessage;
import com.kieral.cryptomon.messaging.model.BalanceMessage;
import com.kieral.cryptomon.messaging.model.ExchangeStatusMessage;
import com.kieral.cryptomon.messaging.model.OrderBookMessage;
import com.kieral.cryptomon.messaging.model.OrderMessage;
import com.kieral.cryptomon.messaging.model.SubscriptionMessage;
import com.kieral.cryptomon.model.accounting.Balance;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.exchange.ExchangeService;
import com.kieral.cryptomon.service.util.FixedSizedList;

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

	private static final Comparator<BalanceEntryMessage> balanceComparator = Comparator.comparing(BalanceEntryMessage::getCurrency);

	private static Comparator<Order> orderComparator = Comparator.comparing(Order::getCreatedTime);
	
	@Autowired 
	ExchangeManagerService exchangeManager;
	@Autowired
	BalanceService balanceService;
	@Autowired
	OrderService orderService;

	private final List<ExchangeService> exchanges = new ArrayList<ExchangeService>();
	private final ConcurrentMap<String, ConcurrentMap<String, OrderBookMessage>> orderBooks = new ConcurrentHashMap<String, ConcurrentMap<String, OrderBookMessage>>();
	private final ConcurrentMap<String, ConnectionStatus> statuses = new ConcurrentHashMap<String, ConnectionStatus>();
	private final FixedSizedList<ArbInstruction> arbInstructions = new FixedSizedList<ArbInstruction>(25);
	private final List<String> exchangeSubscriptions = Collections.synchronizedList(new ArrayList<String>());
	private final List<String> openOrderSubscriptions = Collections.synchronizedList(new ArrayList<String>());
	private final List<String> arbInstructionSubscriptions = Collections.synchronizedList(new ArrayList<String>());

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
    	orderService.registerOrderListener(order -> {
    		if (order != null && order.getMarket() != null) {
    			template.convertAndSend("/topic/openOrders", this.getOpenOrders(order.getMarket()));
    			template.convertAndSend("/topic/exchangeStatus", getExchangeStatus(order.getMarket()));
    		}
    	});
    	// TODO: register with arb monitor
    	// TODO: only hold last n of each list
    }

    @MessageMapping("/exchangeStatus")
    @SendTo("/topic/exchangeStatus")
    public List<ExchangeStatusMessage> subscribeOrderBooks(SubscriptionMessage subscription) {
    	if (!exchangeSubscriptions.contains(subscription.getMarket()))
    		exchangeSubscriptions.add(subscription.getMarket());
        return getExchangeStatus(subscription.getMarket());
    }

    @MessageMapping("/openOrders")
    @SendTo("/topic/openOrders")
    public List<OrderMessage> subscribeLiveOrders(SubscriptionMessage subscription) {
    	if (!openOrderSubscriptions.contains(subscription.getMarket()))
    		openOrderSubscriptions.add(subscription.getMarket());
        return getOpenOrders(subscription.getMarket());
    }

    @MessageMapping("/arbInstructions")
    @SendTo("/topic/arbInstructions")
    public List<ArbStatusMessage> subscribeArbInstructions(SubscriptionMessage subscription) {
    	if (!arbInstructionSubscriptions.contains(subscription.getMarket()))
    		arbInstructionSubscriptions.add(subscription.getMarket());
        return getArbInstructions();
    }


    @Scheduled(fixedRate = 2000)
    public void publishUpdates() {
    	exchangeSubscriptions.forEach(market -> {
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
    			boolean tradingEnabled = exchange.isTradingEnabled();
    			BalanceMessage balances = balanceMessageFor(name, balanceService.getBalances(name));
    			rtn.add(new ExchangeStatusMessage(name, connected, tradingEnabled, balances, orderBookMessages, 
    					ordersFor(orderService.getOpenOrders(name)), ordersFor(orderService.getClosedOrders(name))));
    		}
    	});
    	return rtn;
    }

    private List<OrderMessage> getOpenOrders(String market) {
    	List<OrderMessage> rtn = new ArrayList<OrderMessage>();
    	exchanges.forEach(exchange -> {
    		if ("ALL".equalsIgnoreCase(market) || market.equalsIgnoreCase(exchange.getName())) {
    			String name = exchange.getName();
    			rtn.addAll(ordersFor(orderService.getOpenOrders(name)));
    		}
    	});
    	return rtn;
    }

    private List<ArbStatusMessage> getArbInstructions() {
    	List<ArbStatusMessage> rtn = new ArrayList<ArbStatusMessage>();
		if (arbInstructions.size() > 0) {
			rtn.addAll(arbsFor(arbInstructions));
		}
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
    
    
    Function<Order, OrderMessage> orderMessageFromOrder = new Function<Order, OrderMessage>() {
    	public OrderMessage apply(Order order) {
    		return new OrderMessage(order);
    	}
    };
    
    private List<OrderMessage> ordersFor(List<Order> orders) {
    	if (orders == null)
    		return Collections.emptyList();
    	int maxSize = orders.size() > 20 ? 20 : orders.size();
    	return orders.stream()
    			.sorted(orderComparator.reversed())
    			.map(orderMessageFromOrder)
    			.collect(Collectors.<OrderMessage>toList()).subList(0, maxSize);
    }
    
    Function<ArbInstruction, ArbStatusMessage> arbMessageFromArb = new Function<ArbInstruction, ArbStatusMessage>() {
    	public ArbStatusMessage apply(ArbInstruction arb) {
    		return new ArbStatusMessage(arb);
    	}
    };
    
    private List<ArbStatusMessage> arbsFor(List<ArbInstruction> arbs) {
    	if (arbs == null)
    		return Collections.emptyList();
    	return arbs.stream()
    			.map(arbMessageFromArb)
    			.collect(Collectors.<ArbStatusMessage>toList());
    }

}
