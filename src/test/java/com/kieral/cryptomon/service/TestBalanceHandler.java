package com.kieral.cryptomon.service;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.service.exception.NotEnoughFundsException;

public class TestBalanceHandler {

	private static final String POL = "poloniex";
	private static final String BIT = "bittrex";
	private static final String GD = "gdax";
	
	BalanceHandler balanceHandler;
	
	@Before
	public void setUp() {
		balanceHandler = new BalanceHandler();
		balanceHandler.setConfirmedBalance(POL, Currency.BTC, new BigDecimal(1), true);
		balanceHandler.setConfirmedBalance(POL, Currency.LTC, new BigDecimal(50), true);
		balanceHandler.setConfirmedBalance(POL, Currency.ETH, new BigDecimal(10), true);
		balanceHandler.setConfirmedBalance(BIT, Currency.BTC, new BigDecimal(2), true);
		balanceHandler.setConfirmedBalance(BIT, Currency.LTC, new BigDecimal(60), true);
		balanceHandler.setConfirmedBalance(BIT, Currency.ETH, new BigDecimal(20), true);
		balanceHandler.setConfirmedBalance(GD, Currency.BTC, new BigDecimal(3), true);
		balanceHandler.setConfirmedBalance(GD, Currency.LTC, new BigDecimal(70), true);
		balanceHandler.setConfirmedBalance(GD, Currency.ETH, new BigDecimal(30), true);
	}
	
	@Test
	public void testInitialBalances() {
		balanceHandler.getPrettyPrint(true);
		assertValues("1", balanceHandler.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceHandler.getConfirmedAmount(POL, Currency.LTC));
		assertValues("10", balanceHandler.getConfirmedAmount(POL, Currency.ETH));
		assertValues("2", balanceHandler.getConfirmedAmount(BIT, Currency.BTC));
		assertValues("60", balanceHandler.getConfirmedAmount(BIT, Currency.LTC));
		assertValues("20", balanceHandler.getConfirmedAmount(BIT, Currency.ETH));
		assertValues("3", balanceHandler.getConfirmedAmount(GD, Currency.BTC));
		assertValues("70", balanceHandler.getConfirmedAmount(GD, Currency.LTC));
		assertValues("30", balanceHandler.getConfirmedAmount(GD, Currency.ETH));
		assertValues("1", balanceHandler.getWorkingAmount(POL, Currency.BTC));
		assertValues("50", balanceHandler.getWorkingAmount(POL, Currency.LTC));
		assertValues("10", balanceHandler.getWorkingAmount(POL, Currency.ETH));
		assertValues("2", balanceHandler.getWorkingAmount(BIT, Currency.BTC));
		assertValues("60", balanceHandler.getWorkingAmount(BIT, Currency.LTC));
		assertValues("20", balanceHandler.getWorkingAmount(BIT, Currency.ETH));
		assertValues("3", balanceHandler.getWorkingAmount(GD, Currency.BTC));
		assertValues("70", balanceHandler.getWorkingAmount(GD, Currency.LTC));
		assertValues("30", balanceHandler.getWorkingAmount(GD, Currency.ETH));
	}
	
	@Test
	public void testAdjustedBalances() throws NotEnoughFundsException {
		balanceHandler.getPrettyPrint(true);
		balanceHandler.adiustWorkingAmount(POL, Currency.BTC, bd("0.001"));
		balanceHandler.adiustWorkingAmount(POL, Currency.LTC, bd("-1.21"));
		balanceHandler.adiustWorkingAmount(POL, Currency.ETH, bd("3.33"));
		balanceHandler.adiustWorkingAmount(BIT, Currency.BTC, bd("-1.99"));
		balanceHandler.adiustWorkingAmount(BIT, Currency.LTC, bd("-60.00"));
		balanceHandler.adiustWorkingAmount(BIT, Currency.ETH, bd("12"));
		balanceHandler.adiustWorkingAmount(GD, Currency.BTC, bd("12.00"));
		balanceHandler.adiustWorkingAmount(GD, Currency.LTC, bd("0.001"));
		balanceHandler.adiustWorkingAmount(GD, Currency.ETH, bd("-0.001"));
		balanceHandler.getPrettyPrint(true);
		assertValues("1", balanceHandler.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceHandler.getConfirmedAmount(POL, Currency.LTC));
		assertValues("10", balanceHandler.getConfirmedAmount(POL, Currency.ETH));
		assertValues("2", balanceHandler.getConfirmedAmount(BIT, Currency.BTC));
		assertValues("60", balanceHandler.getConfirmedAmount(BIT, Currency.LTC));
		assertValues("20", balanceHandler.getConfirmedAmount(BIT, Currency.ETH));
		assertValues("3", balanceHandler.getConfirmedAmount(GD, Currency.BTC));
		assertValues("70", balanceHandler.getConfirmedAmount(GD, Currency.LTC));
		assertValues("30", balanceHandler.getConfirmedAmount(GD, Currency.ETH));
		assertValues("1.001", balanceHandler.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceHandler.getWorkingAmount(POL, Currency.LTC));
		assertValues("13.33", balanceHandler.getWorkingAmount(POL, Currency.ETH));
		assertValues("0.01", balanceHandler.getWorkingAmount(BIT, Currency.BTC));
		assertValues("0", balanceHandler.getWorkingAmount(BIT, Currency.LTC));
		assertValues("32", balanceHandler.getWorkingAmount(BIT, Currency.ETH));
		assertValues("15", balanceHandler.getWorkingAmount(GD, Currency.BTC));
		assertValues("70.001", balanceHandler.getWorkingAmount(GD, Currency.LTC));
		assertValues("29.999", balanceHandler.getWorkingAmount(GD, Currency.ETH));
		balanceHandler.adiustWorkingAmount(POL, Currency.BTC, bd("0.001"));
		balanceHandler.getPrettyPrint(true);
		assertValues("1", balanceHandler.getConfirmedAmount(POL, Currency.BTC));
		assertValues("1.002", balanceHandler.getWorkingAmount(POL, Currency.BTC));
		balanceHandler.adiustWorkingAmount(POL, Currency.BTC, bd("-0.003"));
		balanceHandler.getPrettyPrint(true);
		assertValues("1", balanceHandler.getConfirmedAmount(POL, Currency.BTC));
		assertValues("0.999", balanceHandler.getWorkingAmount(POL, Currency.BTC));
	}

	@Test(expected=NotEnoughFundsException.class)
	public void testCantWitdrawExcess() throws NotEnoughFundsException {
		balanceHandler.adiustWorkingAmount(POL, Currency.BTC, bd("-1.0000000000000000001"));
	}

	@Test(expected=NotEnoughFundsException.class)
	public void testThreadSafeCantWitdrawExcess() throws Exception {
		ExecutorService processor = Executors.newFixedThreadPool(11);
		final AtomicReference<Exception> exception = new AtomicReference<Exception>(); 
		final CountDownLatch latch = new CountDownLatch(11); 
		for (int i=0; i<11; i++) {
			processor.submit(() -> {
				try {
					balanceHandler.adiustWorkingAmount(POL, Currency.BTC, bd("-0.1"));
				} catch (Exception e) {
					if (!exception.compareAndSet(null, e))
						exception.set(new IllegalStateException("Only expected one NotEnoughFundsException"));
				} finally {
					latch.countDown();
				}
			});
		}
		latch.await(5000, TimeUnit.MILLISECONDS);
		if (latch.getCount() > 0)
			fail(String.format("Expected all 11 adjustments to happen but still %s remaining", latch.getCount()));
		balanceHandler.getPrettyPrint(true);
		assertValues("0", balanceHandler.getWorkingAmount(POL, Currency.BTC));
		if (exception.get() != null)
			throw exception.get();
	}
	
	@Test
	public void testReconConfirmedBalancesRestoreWorkingBalances() throws NotEnoughFundsException {
		balanceHandler.getPrettyPrint(true);
		balanceHandler.adiustWorkingAmount(POL, Currency.BTC, bd("0.001"));
		balanceHandler.adiustWorkingAmount(POL, Currency.LTC, bd("-1.21"));
		balanceHandler.adiustWorkingAmount(GD, Currency.BTC, bd("12.00"));
		balanceHandler.getPrettyPrint(true);
		assertValues("1", balanceHandler.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceHandler.getConfirmedAmount(POL, Currency.LTC));
		assertValues("3", balanceHandler.getConfirmedAmount(GD, Currency.BTC));
		assertValues("1.001", balanceHandler.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceHandler.getWorkingAmount(POL, Currency.LTC));
		assertValues("15", balanceHandler.getWorkingAmount(GD, Currency.BTC));
		balanceHandler.setConfirmedBalance(POL, Currency.BTC, bd("2"), true);
		balanceHandler.getPrettyPrint(true);
		assertValues("2", balanceHandler.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceHandler.getConfirmedAmount(POL, Currency.LTC));
		assertValues("3", balanceHandler.getConfirmedAmount(GD, Currency.BTC));
		assertValues("2", balanceHandler.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceHandler.getWorkingAmount(POL, Currency.LTC));
		assertValues("15", balanceHandler.getWorkingAmount(GD, Currency.BTC));
	}

	@Test
	public void testNonReconConfirmedBalancesNotRestoreWorkingBalances() throws NotEnoughFundsException {
		balanceHandler.getPrettyPrint(true);
		balanceHandler.adiustWorkingAmount(POL, Currency.BTC, bd("0.001"));
		balanceHandler.adiustWorkingAmount(POL, Currency.LTC, bd("-1.21"));
		balanceHandler.adiustWorkingAmount(GD, Currency.BTC, bd("12.00"));
		balanceHandler.getPrettyPrint(true);
		assertValues("1", balanceHandler.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceHandler.getConfirmedAmount(POL, Currency.LTC));
		assertValues("3", balanceHandler.getConfirmedAmount(GD, Currency.BTC));
		assertValues("1.001", balanceHandler.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceHandler.getWorkingAmount(POL, Currency.LTC));
		assertValues("15", balanceHandler.getWorkingAmount(GD, Currency.BTC));
		balanceHandler.setConfirmedBalance(POL, Currency.BTC, bd("2"), false);
		balanceHandler.getPrettyPrint(true);
		assertValues("2", balanceHandler.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceHandler.getConfirmedAmount(POL, Currency.LTC));
		assertValues("3", balanceHandler.getConfirmedAmount(GD, Currency.BTC));
		assertValues("1.001", balanceHandler.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceHandler.getWorkingAmount(POL, Currency.LTC));
		assertValues("15", balanceHandler.getWorkingAmount(GD, Currency.BTC));
	}

	private static void assertValues(String expected, BigDecimal underTest) {
		if (expected == null && underTest == null)
			return;
		if (expected == null)
			fail("Expected null but received " + underTest.toPlainString());
		assertEquals(String.format("Expected %s but received %s", expected, underTest == null ? "null" : underTest.toPlainString()),
				0, new BigDecimal(expected).compareTo(underTest));
	}
	
	private static final BigDecimal bd(String val) {
		return new BigDecimal(val);
	}

}
