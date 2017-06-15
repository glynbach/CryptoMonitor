package com.kieral.cryptomon.model.orderbook;

import java.math.BigDecimal;

import com.kieral.cryptomon.model.general.Side;

public class OrderBookUpdate {

	private final OrderBookEntry entry;
	private final OrderBookAction action;

	public OrderBookUpdate(Side side, BigDecimal price, BigDecimal amount, OrderBookAction action) {
		this.entry = new OrderBookEntryImpl(side, price, amount == null ? BigDecimal.ZERO : amount);
		this.action = action;
	}

	public OrderBookUpdate(OrderBookEntry entry, OrderBookAction action) {
		this.entry = entry;
		this.action = action;
	}

	public OrderBookEntry getEntry() {
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

	public static class OrderBookEntryImpl implements OrderBookEntry {

		final Side side;
		final BigDecimal price;
		BigDecimal amount;

		public OrderBookEntryImpl(Side side, BigDecimal price) {
			super();
			this.side = side;
			this.price = price;
		}

		public OrderBookEntryImpl(Side side, BigDecimal price, BigDecimal amount) {
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
			OrderBookEntryImpl other = (OrderBookEntryImpl) obj;
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
			return "OrderBookEntryImpl [side=" + side + ", price=" + price + ", amount=" + amount + "]";
		}
		
	}
	
}
