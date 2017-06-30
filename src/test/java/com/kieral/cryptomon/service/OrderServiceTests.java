package com.kieral.cryptomon.service;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;

public class OrderServiceTests {

	MockOrderService os;

	@Before
	public void setUo() {
		os = new MockOrderService();
	}

	@Test
	public void testRequestsOpenOrdersOnTradingEnabled() {
		assertTrue(os.getTradingStatusListeners().containsKey(MockOrderService.BIT));
		assertTrue(os.getTradingStatusListeners().containsKey(MockOrderService.POL));
		os.getTradingStatusListeners().get(MockOrderService.BIT).onTradingEnabled(false);
		Mockito.verify(os.getBittrexMock(), Mockito.after(100).never()).getOpenOrders();
		os.getTradingStatusListeners().get(MockOrderService.POL).onTradingEnabled(false);
		Mockito.verify(os.getPoloniexMock(), Mockito.after(100).never()).getOpenOrders();
		os.getTradingStatusListeners().get(MockOrderService.BIT).onTradingEnabled(true);
		Mockito.verify(os.getBittrexMock(), Mockito.after(100).atLeastOnce()).getOpenOrders();
		Mockito.verify(os.getBittrexMock(), Mockito.after(100).atMost(1)).getOpenOrders();
		Mockito.verify(os.getPoloniexMock(), Mockito.after(100).never()).getOpenOrders();
		os.getTradingStatusListeners().get(MockOrderService.POL).onTradingEnabled(true);
		Mockito.verify(os.getPoloniexMock(), Mockito.after(100).atLeastOnce()).getOpenOrders();
		Mockito.verify(os.getPoloniexMock(), Mockito.after(100).atMost(1)).getOpenOrders();
	}

	@Test
	public void failedPlaceOrderPropagatesCancelStatus() throws InterruptedException {
		os.setDefaultDesiredOrderId(null);
		Order newOrder = MockOrderService.newOrder(MockOrderService.BIT);
		os.placeOrder(newOrder);
		Order order = os.pollOrderUpdate(1000);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertEquals(OrderStatus.PENDING, order.getOrderStatus());
		order = os.pollOrderUpdate(1000);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertEquals(OrderStatus.CANCELLED, order.getOrderStatus());
	}
	
	@Test
	public void placeOrderPropagatesOpenStatus() throws InterruptedException {
		os.setDesiredOrderStatuses(OrderStatus.OPEN);
		os.setDefaultDesiredOrderId("AAA");
		os.placeOrder(MockOrderService.newOrder(MockOrderService.BIT));
		Order order = os.pollOrderUpdate(1000);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertEquals(OrderStatus.PENDING, order.getOrderStatus());
		order = os.pollOrderUpdate(1000);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNotNull(order.getOrderId());
		assertEquals(OrderStatus.OPEN, order.getOrderStatus());
		assertEquals("AAA", order.getOrderId());
	}

	@Test
	public void checkOrderPropagatesNewStatus() throws InterruptedException, OrderNotExistsException {
		os.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		os.setDefaultDesiredOrderId("AAA");
		os.placeOrder(MockOrderService.newOrder(MockOrderService.BIT));
		Order order = os.pollOrderUpdate(1000);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNull(order.getOrderId());
		assertEquals(OrderStatus.PENDING, order.getOrderStatus());
		order = os.pollOrderUpdate(1000);
		assertNotNull(order);
		assertNotNull(order.getClientOrderId());
		assertNotNull(order.getOrderId());
		assertEquals(OrderStatus.OPEN, order.getOrderStatus());
		assertEquals("AAA", order.getOrderId());
		os.setDesiredOrderStatus(order.getClientOrderId(), OrderStatus.PARTIALLY_FILLED);
		os.checkStatus(MockOrderService.BIT, order.getClientOrderId());
		Order updatedOrder = os.pollOrderUpdate(1000);
		assertNotNull(updatedOrder);
		assertEquals(order.getClientOrderId(), updatedOrder.getClientOrderId());
		assertEquals(order.getOrderId(), updatedOrder.getOrderId());
		assertEquals(OrderStatus.PARTIALLY_FILLED, updatedOrder.getOrderStatus());
	}

	@Test
	public void failedOrderDetectsOpen() throws InterruptedException {
		os.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		os.setDefaultDesiredOrderId("AAA");
		os.placeOrder(MockOrderService.newOrder(MockOrderService.BIT));
		Order firstOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.PENDING, firstOrder.getOrderStatus());
		firstOrder = os.pollOrderUpdate(1000);
		assertEquals("AAA", firstOrder.getOrderId());
		assertEquals(OrderStatus.OPEN, firstOrder.getOrderStatus());
		os.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		os.setDefaultDesiredOrderId(null);
		Order exchangeOrder1 = MockOrderService.newOrder(MockOrderService.BIT);
		exchangeOrder1.setOrderId(firstOrder.getOrderId());
		Order exchangeOrder2 = MockOrderService.newOrder(MockOrderService.BIT, "LTCBTC");
		exchangeOrder2.setOrderId("BBB");
		exchangeOrder2.setOrderStatus(OrderStatus.OPEN);
		os.addDesiredExchangeOpenOrder(exchangeOrder1);
		os.addDesiredExchangeOpenOrder(exchangeOrder2);
		os.placeOrder(MockOrderService.newOrder(MockOrderService.BIT, "LTCBTC"));
		Order secondOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.PENDING, secondOrder.getOrderStatus());
		secondOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.OPEN, secondOrder.getOrderStatus());
		assertEquals("BBB", secondOrder.getOrderId());
	}

	@Test
	public void failedOrderDetectsCancelled() throws InterruptedException {
		os.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		os.setDefaultDesiredOrderId("AAA");
		os.placeOrder(MockOrderService.newOrder(MockOrderService.BIT));
		Order firstOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.PENDING, firstOrder.getOrderStatus());
		firstOrder = os.pollOrderUpdate(1000);
		assertEquals("AAA", firstOrder.getOrderId());
		assertEquals(OrderStatus.OPEN, firstOrder.getOrderStatus());
		os.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		os.setDefaultDesiredOrderId(null);
		Order exchangeOrder1 = MockOrderService.newOrder(MockOrderService.BIT);
		exchangeOrder1.setOrderId(firstOrder.getOrderId());
		os.addDesiredExchangeOpenOrder(exchangeOrder1);
		os.placeOrder(MockOrderService.newOrder(MockOrderService.BIT, "LTCBTC"));
		Order secondOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.PENDING, secondOrder.getOrderStatus());
		secondOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.CANCELLED, secondOrder.getOrderStatus());
		assertNull(secondOrder.getOrderId());
	}

	@Test
	public void failedOrderDoesntAssumeWrongOrphanMeansIsOpen() throws InterruptedException {
		os.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		os.setDefaultDesiredOrderId("AAA");
		os.placeOrder(MockOrderService.newOrder(MockOrderService.BIT));
		Order firstOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.PENDING, firstOrder.getOrderStatus());
		firstOrder = os.pollOrderUpdate(1000);
		assertEquals("AAA", firstOrder.getOrderId());
		assertEquals(OrderStatus.OPEN, firstOrder.getOrderStatus());
		os.setDefaultDesiredOrderStatus(OrderStatus.OPEN);
		os.setDefaultDesiredOrderId(null);
		Order exchangeOrder1 = MockOrderService.newOrder(MockOrderService.BIT);
		exchangeOrder1.setOrderId(firstOrder.getOrderId());
		Order exchangeOrder2 = MockOrderService.newOrder(MockOrderService.POL, "LTCBTC", "0.01", "0.123567", false);
		exchangeOrder2.setOrderId("BBB");
		exchangeOrder2.setOrderStatus(OrderStatus.OPEN);
		Order exchangeOrder3 = MockOrderService.newOrder(MockOrderService.POL, "LTCBTC", "0.01", "0.123568", true);
		exchangeOrder3.setOrderId("BBB");
		exchangeOrder3.setOrderStatus(OrderStatus.OPEN);
		Order exchangeOrder4 = MockOrderService.newOrder(MockOrderService.POL, "LTCBTC", "0.02", "0.123567", true);
		exchangeOrder4.setOrderId("BBB");
		exchangeOrder4.setOrderStatus(OrderStatus.OPEN);
		Order exchangeOrder5 = MockOrderService.newOrder(MockOrderService.POL, "ETHBTC", "0.01", "0.123567", true);
		exchangeOrder5.setOrderId("BBB");
		exchangeOrder5.setOrderStatus(OrderStatus.OPEN);
		Order exchangeOrder6 = MockOrderService.newOrder(MockOrderService.BIT, "LTCBTC", "0.01", "0.123567", true);
		exchangeOrder6.setOrderId("BBB");
		exchangeOrder6.setOrderStatus(OrderStatus.OPEN);
		os.addDesiredExchangeOpenOrder(exchangeOrder1);
		os.addDesiredExchangeOpenOrder(exchangeOrder2);
		os.addDesiredExchangeOpenOrder(exchangeOrder3);
		os.addDesiredExchangeOpenOrder(exchangeOrder4);
		os.addDesiredExchangeOpenOrder(exchangeOrder5);
		os.addDesiredExchangeOpenOrder(exchangeOrder6);
		os.placeOrder(MockOrderService.newOrder(MockOrderService.POL, "LTCBTC", "0.01", "0.123567", true));
		Order secondOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.PENDING, secondOrder.getOrderStatus());
		secondOrder = os.pollOrderUpdate(1000);
		assertEquals(OrderStatus.CANCELLED, secondOrder.getOrderStatus());
		assertNull(secondOrder.getOrderId());
	}
	
}
