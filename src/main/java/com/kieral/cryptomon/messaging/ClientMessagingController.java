package com.kieral.cryptomon.messaging;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.kieral.cryptomon.messaging.model.ArbProspectMessage;
import com.kieral.cryptomon.messaging.model.ArbStatusMessage;
import com.kieral.cryptomon.messaging.model.BackOfficeBalanceUpdateMessage;
import com.kieral.cryptomon.messaging.model.BackOfficeUpdateMessage;
import com.kieral.cryptomon.messaging.model.BalanceEntryMessage;
import com.kieral.cryptomon.messaging.model.BalanceMessage;
import com.kieral.cryptomon.messaging.model.ExchangeStatusMessage;
import com.kieral.cryptomon.messaging.model.MarketArbProspectMessage;
import com.kieral.cryptomon.messaging.model.MarketCurrencyArbProspectMessage;
import com.kieral.cryptomon.messaging.model.MarketBidPriceMessage;
import com.kieral.cryptomon.messaging.model.OrderBookMessage;
import com.kieral.cryptomon.messaging.model.OrderMessage;
import com.kieral.cryptomon.messaging.model.SubscriptionMessage;
import com.kieral.cryptomon.model.accounting.Balance;
import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.model.general.LiquidityEntry;
import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.orderbook.OrderBook;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.Trade;
import com.kieral.cryptomon.service.BackOfficeArbSummary;
import com.kieral.cryptomon.service.BackOfficeService;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbInstruction.ArbInstructionLeg;
import com.kieral.cryptomon.service.arb.ArbService;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.exchange.ExchangeService;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.util.CommonUtils;
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
	@Autowired 
	ArbService arbService;
	@Autowired 
	BackOfficeService backOfficeService;
	@Autowired 
	OrderBookConfig orderBookConfig;

	private final List<ExchangeService> exchanges = new ArrayList<ExchangeService>();
	private final ConcurrentMap<String, ConcurrentMap<String, OrderBook>> books = new ConcurrentHashMap<String, ConcurrentMap<String, OrderBook>>();
	private final ConcurrentMap<String, ConcurrentMap<String, OrderBookMessage>> orderBooks = new ConcurrentHashMap<String, ConcurrentMap<String, OrderBookMessage>>();
	private final ConcurrentMap<String, ConnectionStatus> statuses = new ConcurrentHashMap<String, ConnectionStatus>();
	private final List<ArbInstruction> arbInstructions = new FixedSizedList<ArbInstruction>(25);
	private final List<String> exchangeSubscriptions = Collections.synchronizedList(new ArrayList<String>());
	private final List<String> openOrderSubscriptions = Collections.synchronizedList(new ArrayList<String>());
	private final List<String> arbInstructionSubscriptions = Collections.synchronizedList(new ArrayList<String>());
	private final List<BackOfficeUpdateMessage> backOfficeUpdates = new FixedSizedList<BackOfficeUpdateMessage>(25);

    @Autowired
    private SimpMessagingTemplate template;
    
    @PostConstruct
    public void init(){
    	exchangeManager.getEnabledExchanges().forEach(service -> {
    		exchanges.add(service);
    		service.registerOrderBookListener(orderBook -> {
    			books.putIfAbsent(service.getName(), new ConcurrentHashMap<String, OrderBook>());
    			books.get(service.getName()).put(orderBook.getCurrencyPair().getName(), orderBook);
    			orderBooks.putIfAbsent(service.getName(), new ConcurrentHashMap<String, OrderBookMessage>());
    			orderBooks.get(service.getName()).put(orderBook.getCurrencyPair().getName(), new OrderBookMessage(orderBook));
    			sendArbProspects();
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
    	backOfficeService.registerListener(summary -> {
    		if (summary != null) {
    			addBackOfficeUpdate(summary);
    			template.convertAndSend("/topic/backOfficeUpdates", getBackOfficeUpdates());
    		}
    	});
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

    @MessageMapping("/backOfficeUpdates")
    @SendTo("/topic/backOfficeUpdates")
    public List<BackOfficeUpdateMessage> subscribeBackOfficeUpdates(SubscriptionMessage subscription) {
        return getBackOfficeUpdates();
    }

    @MessageMapping("/arbProspects")
    @SendTo("/topic/arbProspects")
    public ArbProspectMessage subscribeArbProspects(SubscriptionMessage subscription) {
        return getArbProspects();
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
    			rtn.add(new ExchangeStatusMessage(name, connected, tradingEnabled, arbService.isSuspended(), balances, orderBookMessages, 
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

    private void addBackOfficeUpdate(BackOfficeArbSummary summary) {
		List<Balance> openingBalances = balanceService.getOpeningBalances();
		Map<Currency, BigDecimal> consolidatedOpeningBalances = new HashMap<Currency, BigDecimal>();
		openingBalances.forEach(balance -> {
			if (!consolidatedOpeningBalances.containsKey(balance.getCurrency()))
				consolidatedOpeningBalances.put(balance.getCurrency(), balance.getConfirmedAmount());
			else
				consolidatedOpeningBalances.put(balance.getCurrency(), consolidatedOpeningBalances.get(balance.getCurrency()).add(balance.getConfirmedAmount()));
		}); 
		List<BackOfficeBalanceUpdateMessage> balanceMessages = new ArrayList<BackOfficeBalanceUpdateMessage>();
		consolidatedOpeningBalances.keySet().forEach(currency -> {
			Balance currentBalance = balanceService.getNetBalance(currency);
			balanceMessages.add(new BackOfficeBalanceUpdateMessage(currency.name(), 
					consolidatedOpeningBalances.get(currency).setScale(8, RoundingMode.HALF_DOWN).toPlainString(), 
					currentBalance.getConfirmedAmount().setScale(8, RoundingMode.HALF_DOWN).toPlainString(),
					currentBalance.getConfirmedAmount().compareTo(consolidatedOpeningBalances.get(currency)) >= 0));
		});
		ArbInstruction instruction = summary.getArbInstruction();
		ArbInstructionLeg longLeg = instruction.getLeg(Side.BID);
		ArbInstructionLeg shortLeg = instruction.getLeg(Side.ASK);
		BackOfficeUpdateMessage updateMessage = new BackOfficeUpdateMessage(longLeg.getCurrencyPair().getName(), 
				longLeg.getMarket(), summary.getLongTrades() == null ? 0 : summary.getLongTrades().size(), 
				getBaseAmountTraded(summary.getLongTrades(), false), getQuotedAmountTraded(summary.getLongTrades(), true),
				longLeg.getCurrencyPair().getBaseCurrency().name(), getAverageRate(summary.getLongTrades(), longLeg.getPrice()), 
				shortLeg.getMarket(), summary.getShortTrades() == null ? 0 : summary.getShortTrades().size(),
				getBaseAmountTraded(summary.getShortTrades(), true), getQuotedAmountTraded(summary.getShortTrades(), false), 
				shortLeg.getCurrencyPair().getQuotedCurrency().name(), getAverageRate(summary.getShortTrades(), shortLeg.getPrice()),
				balanceMessages); 
		backOfficeUpdates.add(updateMessage);
	}
    
    private String getBaseAmountTraded(List<Trade> trades, boolean negate) {
    	BigDecimal total = BigDecimal.ZERO;
    	if (trades != null) {
    		for (Trade trade : trades) {
    			total = total.add(trade.getAmount() == null ? BigDecimal.ZERO : trade.getAmount());
    		}
    	}
    	if (negate)
    		total = total.negate();
    	return total.setScale(8, RoundingMode.HALF_DOWN).toPlainString();
    }

    private String getQuotedAmountTraded(List<Trade> trades, boolean negate) {
    	BigDecimal total = BigDecimal.ZERO;
    	if (trades != null) {
    		for (Trade trade : trades) {
    			total = total.add(trade.getAmount() == null || trade.getRate() == null ? BigDecimal.ZERO : trade.getAmount().multiply(trade.getRate()));
    		}
    	}
    	if (negate)
    		total = total.negate();
    	return total.setScale(8, RoundingMode.HALF_DOWN).toPlainString();
    }

    private String getAverageRate(List<Trade> trades, BigDecimal defaultRate) {
    	BigDecimal total = BigDecimal.ZERO;
    	int count = 0;
    	if (trades != null) {
    		for (Trade trade : trades) {
    			count++;
    			total = total.add(trade.getRate() == null ? defaultRate == null ? BigDecimal.ZERO : defaultRate : trade.getRate());
    		}
    	}
    	if (count == 0)
    		return defaultRate == null ? "0" : defaultRate.toPlainString();
    	return total.divide(new BigDecimal(count), 8, RoundingMode.HALF_UP).toPlainString();
    }

    private List<BackOfficeUpdateMessage> getBackOfficeUpdates() {
		ArrayList<BackOfficeUpdateMessage> rtn = new ArrayList<BackOfficeUpdateMessage>(backOfficeUpdates);
		Collections.reverse(backOfficeUpdates);
		return rtn;
	}

    private void sendArbProspects() {
    	template.convertAndSend("/topic/arbProspects", getArbProspects());
    }
    
    private ArbProspectMessage getArbProspects() {
    	List<MarketArbProspectMessage> arbProspects = new ArrayList<MarketArbProspectMessage>();
    	for (String mkt: books.keySet()) {
			List<MarketCurrencyArbProspectMessage> currencyProspects = new ArrayList<MarketCurrencyArbProspectMessage>();
    		for (String pair : books.get(mkt).keySet()) {
    			OrderBook orderBook = books.get(mkt).get(pair);
    	    	List<MarketBidPriceMessage> bidPrices = new ArrayList<MarketBidPriceMessage>();
		    	LiquidityEntry bookEntry = OrderBookManager.getBestBidAsk(orderBook, orderBookConfig.getSignificantAmount(orderBook.getMarket(), orderBook.getCurrencyPair().getBaseCurrency()));
		    	if (bookEntry == null || bookEntry.getBidAskAmount() == null || bookEntry.getBidAskAmount().get(Side.ASK) == null)
		    		continue;
		    	for (String market: books.keySet()) {
		    		if (market.equals(orderBook.getMarket()))
		    			continue;
		    		if (books.get(market).containsKey(orderBook.getCurrencyPair().getName())) {
		    			OrderBook otherBook = books.get(market).get(orderBook.getCurrencyPair().getName());
		    			LiquidityEntry otherBookEntry = OrderBookManager.getBestBidAsk(otherBook, orderBookConfig.getSignificantAmount(otherBook.getMarket(), otherBook.getCurrencyPair().getBaseCurrency()));
		    			if (otherBookEntry == null || otherBookEntry.getBidAskAmount() == null || otherBookEntry.getBidAskAmount().get(Side.BID) == null)
		    				continue;
		    			BigDecimal commonAmount = bookEntry.getBidAskAmount().get(Side.ASK).getBaseAmount().compareTo(otherBookEntry.getBidAskAmount().get(Side.BID).getBaseAmount()) > 0 ?
		    					otherBookEntry.getBidAskAmount().get(Side.BID).getBaseAmount() : bookEntry.getBidAskAmount().get(Side.ASK).getBaseAmount();
		    			BigDecimal quotedBuyAmount = commonAmount.multiply(bookEntry.getBidAskPrice().get(Side.ASK));
		    			BigDecimal quotedSellAmount = commonAmount.multiply(otherBookEntry.getBidAskPrice().get(Side.BID));
		    			BigDecimal quotedBuyAmountWithFees = quotedBuyAmount.multiply(CommonUtils.getTradingfeeMultiplier(Side.BID, orderBook.getCurrencyPair().getTradingFee()));
		    			BigDecimal quotedSellAmountWithFees = quotedSellAmount.multiply(CommonUtils.getTradingfeeMultiplier(Side.ASK, otherBook.getCurrencyPair().getTradingFee()));
		    			boolean opportunity = quotedSellAmountWithFees.compareTo(quotedBuyAmountWithFees) > 0;
		    			bidPrices.add(new MarketBidPriceMessage(otherBook.getMarket(), otherBookEntry.getBidAskPrice().get(Side.BID).toPlainString(), commonAmount.toPlainString(), 
		    					quotedSellAmount.subtract(quotedBuyAmount).setScale(8, RoundingMode.HALF_DOWN).toPlainString(),
		    					quotedSellAmountWithFees.subtract(quotedBuyAmountWithFees).setScale(8, RoundingMode.HALF_DOWN).toPlainString(),
		    					opportunity));
		    		}
		    	}
		    	currencyProspects.add(new MarketCurrencyArbProspectMessage(orderBook.getMarket(), orderBook.getCurrencyPair().getName(), 
		    			bookEntry.getBidAskPrice().get(Side.ASK).toPlainString(), bidPrices));
		    	currencyProspects.sort(Comparator.comparing(MarketCurrencyArbProspectMessage::getCurrencyPair));
    		}
	    	arbProspects.add(new MarketArbProspectMessage(mkt, currencyProspects));
    	}
    	return new ArbProspectMessage(arbProspects);
    }
    
}
