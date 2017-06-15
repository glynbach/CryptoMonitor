package com.kieral.cryptomon.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import com.kieral.cryptomon.model.accounting.Balance;
import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.service.exception.BalanceUnavailableException;
import com.kieral.cryptomon.service.exception.NotEnoughFundsException;

public class BalanceService {

	private final Comparator<Balance> byMarket = Comparator.comparing(balance -> balance.getMarket());
	private final Comparator<Balance> byCurrency = Comparator.comparing(balance -> balance.getCurrency());
	
	private final ConcurrentMap<BalanceKey, Balance> balances = new ConcurrentHashMap<BalanceKey, Balance>();
	private final ConcurrentMap<BalanceKey, ReentrantLock> locks = new ConcurrentHashMap<BalanceKey, ReentrantLock>();
	
	public void setConfirmedBalance(String market, Currency currency, BigDecimal confirmedAmount, boolean forceReconcileWorkingAmount) {
		BalanceKey key = keyFor(market, currency);
		if (balances.putIfAbsent(key, new Balance(market, currency, confirmedAmount, confirmedAmount)) != null) {
			Lock lock = getLock(market, currency);
			lock.lock();
			try {
				Balance balance = balances.get(key);
				balance.setConfirmedAmount(confirmedAmount);
				if (forceReconcileWorkingAmount)
					balance.setWorkingAmount(confirmedAmount);
			} finally {
				lock.unlock();
			}
		}
	}
	
	public BigDecimal adiustWorkingAmount(String market, Currency currency, BigDecimal amount) throws NotEnoughFundsException {
		if (market == null)
			throw new IllegalArgumentException("market can not be null");
		if (currency == null)
			throw new IllegalArgumentException("currency can not be null");
		if (amount == null)
			throw new IllegalArgumentException("amount can not be null");
		BalanceKey key = keyFor(market, currency);
		Lock lock = getLock(market, currency);
		lock.lock();
		try {
			if (!balances.containsKey(key))
				throw new NotEnoughFundsException(String.format("No balance found for %s in %s", currency, market));
			BigDecimal workingAmount = balances.get(key).getWorkingAmount();
			if (workingAmount == null)
				workingAmount = BigDecimal.ZERO;
			workingAmount = workingAmount.add(amount);
			if (workingAmount.compareTo(BigDecimal.ZERO) < 0)
				throw new NotEnoughFundsException(String.format("Not enough balance left after applying %s to %s", 
						amount, balances.get(key).getWorkingAmount()));
			balances.get(key).setWorkingAmount(workingAmount);
		} finally {
			lock.unlock();
		}
		return null;
	}

	public BigDecimal getWorkingAmount(String market, Currency currency) {
		BalanceKey key = keyFor(market, currency);
		if (balances.containsKey(key))
			return balances.get(key).getWorkingAmount();
		return BigDecimal.ZERO;
		
	}

	public List<Balance> getBalances(String market) {
		if (market == null)
			return Collections.emptyList();
		List<Balance> rtnBalances = new ArrayList<Balance>(); 
		balances.keySet().forEach(key -> {
			if (market.equals(key.getMarket())) {
				rtnBalances.add(balances.get(key));
			}
		});
		return rtnBalances;
	}

	public BigDecimal getConfirmedAmount(String market, Currency currency) {
		BalanceKey key = keyFor(market, currency);
		if (balances.containsKey(key))
			return balances.get(key).getConfirmedAmount();
		return BigDecimal.ZERO;
	}
	
	public void restoreWorkingAmountToConfirmedAmount(String market, Currency currency) throws BalanceUnavailableException {
		if (market == null)
			throw new IllegalArgumentException("market can not be null");
		if (currency == null)
			throw new IllegalArgumentException("currency can not be null");
		BalanceKey key = keyFor(market, currency);
		Lock lock = getLock(market, currency);
		lock.lock();
		try {
			Balance balance = balances.get(key);
			if (balance == null || balance.getConfirmedAmount() == null)
				throw new BalanceUnavailableException(String.format("No confirmed balance avaialable for %s on %s", currency, market));
			balance.setWorkingAmount(balance.getConfirmedAmount());
		} finally {
			lock.unlock();
		}
	}
	
	private Lock getLock(String market, Currency currency) {
		if (market == null)
			throw new IllegalArgumentException("market can not be null");
		if (currency == null)
			throw new IllegalArgumentException("currency can not be null");
		BalanceKey key = keyFor(market, currency);
		locks.putIfAbsent(key, new ReentrantLock());
		return locks.get(key);
	}
	
	private final static BalanceKey keyFor(String market, Currency currency) {
		return new BalanceKey(market, currency);
	}
	
	public static class BalanceKey {
		
		private final String market;
		private final Currency currency;
		
		public BalanceKey(String market, Currency currency) {
			this.market = market;
			this.currency = currency;
		}

		public String getMarket() {
			return market;
		}

		public Currency getCurrency() {
			return currency;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((currency == null) ? 0 : currency.hashCode());
			result = prime * result + ((market == null) ? 0 : market.hashCode());
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
			BalanceKey other = (BalanceKey) obj;
			if (currency != other.currency)
				return false;
			if (market == null) {
				if (other.market != null)
					return false;
			} else if (!market.equals(other.market))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "BalanceKey [market=" + market + ", currency=" + currency + "]";
		}
		
	}

	public List<String> getPrettyPrint(boolean printToConsole) {
		List<Balance> sortedBalances = balances.values().stream()
				.sorted(byMarket.thenComparing(byCurrency)).collect(Collectors.toList());
		List<String> lines = new ArrayList<String>(); 
		final AtomicReference<String> lastMarket = new AtomicReference<String>();
		final AtomicReference<StringBuffer> line1 = new AtomicReference<StringBuffer>();
		final AtomicReference<StringBuffer> line2 = new AtomicReference<StringBuffer>();
		lines.add("Current Working Balances ---------------------------");
		sortedBalances.forEach(balance -> {
			if (!balance.getMarket().equals(lastMarket.get())) {
				if (lastMarket.get() != null) {
					lines.add(lastMarket.get() + ": ");
					lines.add(line1.get().toString());
					lines.add(line2.get().toString());
				}
				lastMarket.set(balance.getMarket());
				line1.set(new StringBuffer(""));
				line2.set(new StringBuffer(""));
			}
			line1.get().append("\t\t").append(balance.getCurrency().name());
			line2.get().append("\t\t").append(balance.getWorkingAmount().toPlainString());
		});
		if (lastMarket.get() != null) {
			lines.add(lastMarket.get() + ": ");
			lines.add(line1.get().toString());
			lines.add(line2.get().toString());
		}
		lines.add("----------------------------------------------------");
		if (printToConsole) {
			lines.forEach(line ->{
				System.out.println(line);
			}); 
		}
		return lines;
	}
	
	@Override
	public String toString() {
		return "BalanceHandler [balances=" + balances + "]";
	}
	
}
