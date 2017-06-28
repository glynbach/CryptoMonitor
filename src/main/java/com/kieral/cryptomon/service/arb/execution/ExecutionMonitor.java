package com.kieral.cryptomon.service.arb.execution;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.util.CommonUtils;
import com.kieral.cryptomon.service.util.TradingUtils;

public class ExecutionMonitor {

	// Todo; pull these from a config
	private static BigDecimal MINIMUM_AMOUNT = new BigDecimal("0.0001");
	private static BigDecimal MINIMUM_PRICE = new BigDecimal("0.00000001");;
	@Autowired
	ExchangeManagerService exchangeManagerService;
	@Autowired
	OrderService orderService;
	
	private final ArbInstruction arbInstruction;
	private final List<Order> leg1Orders = new LinkedList<Order>();
	private final List<Order> leg2Orders = new LinkedList<Order>();
	
	public ExecutionMonitor(final ArbInstruction arbInstruction) {
		validateArbInstruction(arbInstruction);
		this.arbInstruction = arbInstruction;
	}
	
	public OrderStatusSummary getStatus() {
		BigDecimal desiredLeg1Amount = arbInstruction.getLegs().get(0).getAmount().getBaseAmount();
		BigDecimal desiredLeg2Amount = arbInstruction.getLegs().get(1).getAmount().getBaseAmount();
		OrderStatus leg1Status = leg1Orders.size() == 0 
				? OrderStatus.PENDING 
				: leg1Orders.get(leg1Orders.size() - 1).getOrderStatus();
		OrderStatus leg2Status = leg2Orders.size() == 0 
				? OrderStatus.PENDING 
				: leg2Orders.get(leg2Orders.size() - 1).getOrderStatus();
		BigDecimal amount1 = BigDecimal.ZERO;
		for (Order order : leg1Orders)
			amount1 = amount1.add(TradingUtils.getFilledAmount(order));
		BigDecimal amount2 = BigDecimal.ZERO;
		for (Order order : leg2Orders)
			amount2 = amount2.add(TradingUtils.getFilledAmount(order));
		return new OrderStatusSummary(leg1Status, leg2Status, 
				desiredLeg1Amount.subtract(amount1), desiredLeg2Amount.subtract(amount2));
	}
	
	private void validateArbInstruction(ArbInstruction arbInstruction) {
		if (arbInstruction == null)
			throw new IllegalArgumentException("arbInstruction can not be null");
		if (arbInstruction.getLegs() == null || arbInstruction.getLegs().size() != 2)
			throw new IllegalArgumentException("arbInstruction must have two legs");
		for (int i=0; i<2; i++) {
			String market = arbInstruction.getLegs().get(0).getMarket();
			if (market == null || !exchangeManagerService.isTradingEnabled(market))
				throw new IllegalArgumentException("market " + market + " is not available for trading for leg " + (i+1));
//			ServiceExchangeProperties properties = exchangeManagerService.getProperties(market);
//			if (properties == null)
//				throw new IllegalArgumentException("properties not available for " + market + " for leg " + (i+1));
			if (arbInstruction.getLegs().get(0).getAmount() == null) 
				throw new IllegalArgumentException("arbInstruction has no trade amount for leg " + (i+1));
			if (arbInstruction.getLegs().get(0).getAmount().getBaseAmount() == null || 
					!CommonUtils.isAtLeast(arbInstruction.getLegs().get(0).getAmount().getBaseAmount(), 
							MINIMUM_AMOUNT)) 
				throw new IllegalArgumentException("arbInstruction amount is invalid for leg " + (i+1));
			if (arbInstruction.getLegs().get(0).getPrice() == null || 
					!CommonUtils.isAtLeast(arbInstruction.getLegs().get(0).getPrice(), 
							MINIMUM_PRICE)) 
				throw new IllegalArgumentException("arbInstruction price is invalid for leg " + (i+1));
			if (arbInstruction.getLegs().get(0).getSide() == null)
				throw new IllegalArgumentException("arbInstruction has no side for leg " + (i+1));
			if (arbInstruction.getLegs().get(0).getCurrencyPair() == null)
				throw new IllegalArgumentException("arbInstruction has no surrency pair for leg " + (i+1));
		}
	}
	
	public static class OrderStatusSummary {
		
		private OrderStatus leg1Status;
		private OrderStatus leg2Status;
		private BigDecimal leg1Shortfall;
		private BigDecimal leg2Shortfall;
		
		public OrderStatusSummary(OrderStatus leg1Status, OrderStatus leg2Status, BigDecimal leg1Shortfall,
				BigDecimal leg2Shortfall) {
			this.leg1Status = leg1Status;
			this.leg2Status = leg2Status;
			this.leg1Shortfall = leg1Shortfall;
			this.leg2Shortfall = leg2Shortfall;
		}

		public OrderStatus getLeg1Status() {
			return leg1Status;
		}

		public void setLeg1Status(OrderStatus leg1Status) {
			this.leg1Status = leg1Status;
		}

		public OrderStatus getLeg2Status() {
			return leg2Status;
		}

		public void setLeg2Status(OrderStatus leg2Status) {
			this.leg2Status = leg2Status;
		}

		public BigDecimal getLeg1Shortfall() {
			return leg1Shortfall;
		}

		public void setLeg1Shortfall(BigDecimal leg1Shortfall) {
			this.leg1Shortfall = leg1Shortfall;
		}

		public BigDecimal getLeg2Shortfall() {
			return leg2Shortfall;
		}

		public void setLeg2Shortfall(BigDecimal leg2Shortfall) {
			this.leg2Shortfall = leg2Shortfall;
		}

		@Override
		public String toString() {
			return "OrderStatusSummary [leg1Status=" + leg1Status + ", leg2Status=" + leg2Status + ", leg1Shortfall="
					+ leg1Shortfall + ", leg2Shortfall=" + leg2Shortfall + "]";
		}
		
	}
}