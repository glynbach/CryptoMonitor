package com.kieral.cryptomon.model;

import java.math.BigDecimal;

public class OrderBookUpdate {

	private final IOrderBookEntry entry;
	private final OrderBookAction action;

	public OrderBookUpdate(String type, String price, String amount, OrderBookAction action) {
		if (type == null)
			throw new IllegalArgumentException("type can not be null");
		Side side = null;
		if (type.equalsIgnoreCase("BID"))
			side = Side.BID;
		if (type.equalsIgnoreCase("ASK"))
			side = Side.ASK;
		if (side == null)
			throw new IllegalArgumentException("Invalid type " + type);
		BigDecimal px = new BigDecimal(price);
		BigDecimal amnt = action == OrderBookAction.REPLACE ? new BigDecimal(amount) : BigDecimal.ZERO;
		this.entry = new OrderBookEntry(side, px, amnt);
		this.action = action;
	}

	public OrderBookUpdate(IOrderBookEntry entry, OrderBookAction action) {
		this.entry = entry;
		this.action = action;
	}

	public IOrderBookEntry getEntry() {
		return entry;
	}

	public OrderBookAction getAction() {
		return action;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((action == null) ? 0 : action.hashCode());
		result = prime * result + ((entry == null) ? 0 : entry.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OrderBookUpdate other = (OrderBookUpdate) obj;
		if (action != other.action)
			return false;
		if (entry == null) {
			if (other.entry != null)
				return false;
		} else if (!entry.equals(other.entry))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OrderBookUpdate [entry=" + entry + ", action=" + action + "]";
	}

	public static class OrderBookEntry implements IOrderBookEntry {

		final Side side;
		final BigDecimal price;
		BigDecimal amount;

		public OrderBookEntry(Side side, BigDecimal price) {
			super();
			this.side = side;
			this.price = price;
		}

		public OrderBookEntry(Side side, BigDecimal price, BigDecimal amount) {
			super();
			this.side = side;
			this.price = price;
			this.amount = amount;
		}

		@Override
		public Side getSide() {
			return side;
		}

		@Override
		public BigDecimal getPrice() {
			return price;
		}

		@Override
		public BigDecimal getAmount() {
			return amount;
		}

		@Override
		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((amount == null) ? 0 : amount.hashCode());
			result = prime * result + ((price == null) ? 0 : price.hashCode());
			result = prime * result + ((side == null) ? 0 : side.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			OrderBookEntry other = (OrderBookEntry) obj;
			if (amount == null) {
				if (other.amount != null)
					return false;
			} else if (!amount.equals(other.amount))
				return false;
			if (price == null) {
				if (other.price != null)
					return false;
			} else if (!price.equals(other.price))
				return false;
			if (side != other.side)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "OrderBookEntry [side=" + side + ", price=" + price + ", amount=" + amount + "]";
		}
		
	}
	
}
