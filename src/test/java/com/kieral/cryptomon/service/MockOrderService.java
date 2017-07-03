package com.kieral.cryptomon.service;

import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.kieral.cryptomon.model.general.Side;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.model.trading.Trade;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.exchange.ExchangeService;
import com.kieral.cryptomon.service.exchange.TradingStatusListener;
import com.kieral.cryptomon.service.util.TradingUtils;
import com.kieral.cryptomon.test.utlil.TestUtils;

public class MockOrderService implements OrderService {
	
	public static String BIT = "bittrex";
	public static String POL = "poloniex";
	public static String GDAX = "gdax";
			
	final OrderService orderService;
	final ExchangeManagerService exchangeManagerService;
	final ExchangeService bittrex;
	final ExchangeService poloniex;
	final ExchangeService gdax;
	
	final Map<String, String> clientOrderIdToMarketMap = new HashMap<String, String>();
	final Map<String, String> desiredOrderIds = new HashMap<String, String>();
	final Map<String, OrderStatus> desiredOrderStatuses = new HashMap<String, OrderStatus>();
	final Map<String, BigDecimal> desiredOrderFills = new HashMap<String, BigDecimal>();
	final List<Order> desiredExchangeOpenOrders = new ArrayList<Order>();
	
	final Map<String, TradingStatusListener> tradingStatusListeners = new HashMap<String, TradingStatusListener>();
	final Map<String, ConnectionStatus> desiredExchangeStatus = new HashMap<String, ConnectionStatus>();
	final Map<String, Boolean> desiredExchangeTradingEnabled = new HashMap<String, Boolean>();

	final BlockingQueue<OrderStatus> actionOrderStatuses = new ArrayBlockingQueue<OrderStatus>(512);
	final BlockingQueue<Order> orderUpdates = new ArrayBlockingQueue<Order>(512);

	public void setDesiredOrderId(String clientOrderId, String orderId) {
		desiredOrderIds.put(clientOrderId, orderId);
	}

	public void setDefaultDesiredOrderId(String orderId) {
		desiredOrderIds.put("DEFAULT", orderId);
	}

	public void setDesiredActionOrderStatuses(OrderStatus... orderStatus) {
		for (OrderStatus status : orderStatus) {
			actionOrderStatuses.add(status);
		}
	}

	public void setDesiredOrderStatus(String clientOrderId, OrderStatus orderStatus) {
		setDesiredOrderStatus(clientOrderId, orderStatus, null);
	}

	public void setDesiredOrderStatus(String clientOrderId, OrderStatus orderStatus, BigDecimal fillAmount) {
		desiredOrderStatuses.put(clientOrderId, orderStatus);
		if (fillAmount != null && fillAmount.compareTo(BigDecimal.ZERO) > 0)
			desiredOrderFills.put(clientOrderId, fillAmount);
	}

	public void clearDesiredOrderStatus() {
		desiredOrderStatuses.clear();
	}
	public void removeDesiredOrderStatus(String clientOrderId) {
		desiredOrderStatuses.remove(clientOrderId);
	}

	public void setDefaultDesiredOrderStatus(OrderStatus orderStatus) {
		desiredOrderStatuses.put("DEFAULT", orderStatus);
	}

	public void addDesiredExchangeOpenOrder(Order order) {
		desiredExchangeOpenOrders.add(order);
	}

	public void setDesiredExchangeOpenOrders(List<Order> orders) {
		desiredExchangeOpenOrders.clear();
		desiredExchangeOpenOrders.addAll(orders);
	}

	public void setDesiredExchangeStatus(String market, ConnectionStatus status) {
		desiredExchangeStatus.put(market, status);
	}

	public void setDesiredExchangeTradingEnabled(String market, boolean enabled) {
		desiredExchangeTradingEnabled.put(market, enabled);
	}

	public ExchangeManagerService getExchangeManagerServiceMock() {
		return exchangeManagerService;
	}
	
	public ExchangeService getBittrexMock() {
		return bittrex;
	}
	
	public ExchangeService getPoloniexMock() {
		return poloniex;
	}
	
	public ExchangeService getGdaxMock() {
		return gdax;
	}
	
	public Order pollOrderUpdate(long timeout) throws InterruptedException {
		return orderUpdates.poll(timeout, TimeUnit.MILLISECONDS);
	}
	
	public Map<String, TradingStatusListener> getTradingStatusListeners() {
		return tradingStatusListeners;
	}
	
	public static Order newOrder(String market) {
		return newOrder(market, null);
	}

	public static Order newOrder(String market, String pair) {
		return newOrder(market, pair, null, null, true);
	}

	public static Order newOrder(String market, String pair, String amount, String price, boolean side) {
		final Order order = new Order(market, pair == null ? "ETHBTC" : pair, amount == null ? new BigDecimal("0.001") : new BigDecimal(amount),
				price == null ? new BigDecimal("0.13875") : new BigDecimal(price), side == true ? Side.BID : Side.ASK);
		order.setCurrencyPair(TestUtils.cpFor(order.getCurrencyPairStr()));
		return order;
	}

	@SuppressWarnings("unchecked")
	public MockOrderService() {
		bittrex = Mockito.mock(ExchangeService.class);
		Mockito.when(bittrex.getName()).thenReturn(BIT);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				tradingStatusListeners.put(BIT, invocation.getArgument(0));
				return null;
			}}).when(bittrex)
			.registerTradingStatusListener(Mockito.any(TradingStatusListener.class));
		poloniex = Mockito.mock(ExchangeService.class);
		Mockito.when(poloniex.getName()).thenReturn(POL);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				tradingStatusListeners.put(POL, invocation.getArgument(0));
				return null;
			}}).when(poloniex)
			.registerTradingStatusListener(Mockito.any(TradingStatusListener.class));
		gdax = Mockito.mock(ExchangeService.class);
		Mockito.when(gdax.getName()).thenReturn(GDAX);
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				tradingStatusListeners.put(GDAX, invocation.getArgument(0));
				return null;
			}}).when(gdax)
			.registerTradingStatusListener(Mockito.any(TradingStatusListener.class));
		List<ExchangeService> services = Arrays.asList(new ExchangeService[] {bittrex, poloniex, gdax});
		exchangeManagerService = Mockito.mock(ExchangeManagerService.class);
		Mockito.when(exchangeManagerService.getEnabledExchanges()).thenReturn(services);
		List<String> serviceNames = services.stream().map(ExchangeService::getName).collect(Collectors.<String>toList());
		Mockito.when(exchangeManagerService.getEnabledExchangeNames()).thenReturn(serviceNames);
		orderService = new OrderServiceImpl();
		Mockito.doAnswer(new Answer<OrderStatus>() {
			@Override
			public OrderStatus answer(InvocationOnMock invocation) throws Throwable {
				Order order = (Order)invocation.getArgument(0);
				OrderStatus orderStatus = getActionOrderStatus(order.getClientOrderId());
				if (OrderStatus.CANCELLED != orderStatus) {
					order.setOrderId(getActionOrderId(order.getClientOrderId()));
				} 
				return orderStatus;
			}}).when(exchangeManagerService).placeOrder(Mockito.any(Order.class));
		Mockito.doAnswer(new Answer<OrderStatus>() {
			@Override
			public OrderStatus answer(InvocationOnMock invocation) throws Throwable {
				Order order = (Order)invocation.getArgument(0);
				OrderStatus orderStatus = getActionOrderStatus(order.getClientOrderId());
				if (OrderStatus.CANCELLED != orderStatus) {
					order.setOrderId(getActionOrderId(order.getClientOrderId()));
				} 
				return orderStatus;
			}}).when(exchangeManagerService).placeMarketOrder(Mockito.any(Order.class));
		Mockito.doAnswer(new Answer<Map<String, OrderStatus>>(){
			@Override
			public Map<String, OrderStatus> answer(InvocationOnMock invocation) throws Throwable {
				List<Order> orders = invocation.getArgument(1);
				Map<String, OrderStatus> rtn = new HashMap<String, OrderStatus>();
				if (orders != null) {
					orders.forEach(order -> {
						String clientOrderId = order.getClientOrderId();
						OrderStatus orderStatus = getOpenOrderStatus(clientOrderId);
						if (OrderStatus.FILLED == orderStatus) {
							fillOrder(true, orderService.getOrder(clientOrderIdToMarketMap.get(clientOrderId), clientOrderId));
						}
						if (OrderStatus.PARTIALLY_FILLED == orderStatus) {
							fillOrder(false, orderService.getOrder(clientOrderIdToMarketMap.get(clientOrderId), clientOrderId), desiredOrderFills.get(clientOrderId));
							if (desiredOrderFills.containsKey(clientOrderId))
								desiredOrderFills.put(clientOrderId, BigDecimal.ZERO);
						}
						rtn.put(clientOrderId, orderStatus);
					});
				}
				return rtn;
			}}).when(exchangeManagerService).getOpenOrderStatuses(Mockito.anyString(), Mockito.any(List.class));
		Mockito.doAnswer(new Answer<List<Order>>(){
			@Override
			public List<Order> answer(InvocationOnMock invocation) throws Throwable {
				return desiredExchangeOpenOrders;
			}}).when(exchangeManagerService).getOpenOrders(Mockito.anyString());
		Mockito.doAnswer(new Answer<OrderStatus>() {
			@Override
			public OrderStatus answer(InvocationOnMock invocation) throws Throwable {
				Order order = (Order)invocation.getArgument(0);
				return getActionOrderStatus(order.getClientOrderId());
			}}).when(exchangeManagerService).cancelOrder(Mockito.any(Order.class));
		Mockito.doAnswer(new Answer<ConnectionStatus>() {
			@Override
			public ConnectionStatus answer(InvocationOnMock invocation) throws Throwable {
				return desiredExchangeStatus.get(invocation.getArgument(0));
			}}).when(exchangeManagerService).getExchangeStatus(Mockito.anyString());
		Mockito.doAnswer(new Answer<Boolean>() {
			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {
				return desiredExchangeTradingEnabled.get(invocation.getArgument(0));
			}}).when(exchangeManagerService).isTradingEnabled(Mockito.anyString());
		ReflectionTestUtils.setField(orderService, "exchangeManagerService", exchangeManagerService);
		desiredExchangeStatus.put(BIT, ConnectionStatus.CONNECTED);
		desiredExchangeStatus.put(POL, ConnectionStatus.CONNECTED);
		desiredExchangeStatus.put(GDAX, ConnectionStatus.DISCONNECTED);
		desiredExchangeTradingEnabled.put(BIT, false);
		desiredExchangeTradingEnabled.put(POL, false);
		desiredExchangeTradingEnabled.put(GDAX, false);
		orderService.registerOrderListener(order -> {
			try {
				clientOrderIdToMarketMap.put(order.getClientOrderId(), order.getMarket());
				orderUpdates.put(new Order(order));
			} catch (Exception e) {
				e.printStackTrace();
				fail("Cant place order " + order + " on test result queue " + e.getMessage());
			}
		});
		orderService.init();
	}

	private OrderStatus getOpenOrderStatus(String clientOrderId) {
		if (desiredOrderStatuses.containsKey(clientOrderId))
			return desiredOrderStatuses.get(clientOrderId);
		if (desiredOrderStatuses.containsKey("DEFAULT"))
			return desiredOrderStatuses.get("DEFAULT");
		return OrderStatus.ERROR;
	}
	
	private OrderStatus getActionOrderStatus(String clientOrderId) {
		if (!actionOrderStatuses.isEmpty())
			return actionOrderStatuses.poll();
		if (desiredOrderStatuses.containsKey(clientOrderId))
			return desiredOrderStatuses.get(clientOrderId);
		if (desiredOrderStatuses.containsKey("DEFAULT"))
			return desiredOrderStatuses.get("DEFAULT");
		return OrderStatus.ERROR;
	}

	private String getActionOrderId(String clientOrderId) {
		if (desiredOrderIds.containsKey(clientOrderId))
			return desiredOrderIds.get(clientOrderId);
		if (desiredOrderIds.containsKey("DEFAULT"))
			return desiredOrderIds.get("DEFAULT");
		return UUID.randomUUID().toString();
	}

	private void fillOrder(boolean fullyFill, Order order) {
		fillOrder(fullyFill, order, null);
	}
		
	private void fillOrder(boolean fullyFill, Order order, BigDecimal amount) {
		BigDecimal filledAmount = TradingUtils.getFilledAmount(order);
		BigDecimal remainingAmount = amount != null ? amount : order.getAmount().subtract(filledAmount)
				.divide(fullyFill ? BigDecimal.ONE : new BigDecimal("2"), 8, RoundingMode.HALF_DOWN);
		if (remainingAmount.compareTo(BigDecimal.ZERO) > 0)
			order.mergeTrades(Arrays.asList(new Trade[]{new Trade(UUID.randomUUID().toString(), order.getPrice(), 
					remainingAmount, new BigDecimal("0.0001"), true, System.currentTimeMillis())}));
	}
	
	@Override
	public boolean isTradingEnabled(String market) {
		return orderService.isTradingEnabled(market);
	}

	@Override
	public void registerOrderListener(OrderListener listener) {
		orderService.registerOrderListener(listener);
	}

	@Override
	public List<Order> getClosedOrders(String market) {
		return orderService.getClosedOrders(market);
	}

	@Override
	public List<Order> getOpenOrders(String market) {
		return orderService.getOpenOrders(market);
	}

	@Override
	public List<Order> getAllOrders(String market) {
		return orderService.getAllOrders(market);
	}

	@Override
	public boolean placeOrder(Order order) {
		return orderService.placeOrder(order);
	}

	@Override
	public boolean placeMarketOrder(Order order) {
		return orderService.placeMarketOrder(order);
	}

	@Override
	public boolean cancelOrder(String market, String clientOrderId) throws OrderNotExistsException {
		return orderService.cancelOrder(market, clientOrderId);
	}

	@Override
	public OrderStatus forceCancelOrder(String market, String orderId) throws OrderNotExistsException {
		return orderService.forceCancelOrder(market, orderId);
	}

	@Override
	public void checkStatus(String market, String clientOrderId) throws OrderNotExistsException {
		orderService.checkStatus(market, clientOrderId);
	}

	@Override
	public void init() {
	}

	@Override
	public Order getOrder(String market, String clientOrderId) {
		return orderService.getOrder(market, clientOrderId);
	}

}
