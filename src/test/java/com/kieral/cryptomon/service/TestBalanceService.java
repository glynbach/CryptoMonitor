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

public class TestBalanceService {

	private static final String POL = "poloniex";
	private static final String BIT = "bittrex";
	private static final String GD = "gdax";
	
	BalanceService balanceService;
	
	@Before
	public void setUp() {
		balanceService = new BalanceService();
		balanceService.setConfirmedBalance(POL, Currency.BTC, new BigDecimal(1), true);
		balanceService.setConfirmedBalance(POL, Currency.LTC, new BigDecimal(50), true);
		balanceService.setConfirmedBalance(POL, Currency.ETH, new BigDecimal(10), true);
		balanceService.setConfirmedBalance(BIT, Currency.BTC, new BigDecimal(2), true);
		balanceService.setConfirmedBalance(BIT, Currency.LTC, new BigDecimal(60), true);
		balanceService.setConfirmedBalance(BIT, Currency.ETH, new BigDecimal(20), true);
		balanceService.setConfirmedBalance(GD, Currency.BTC, new BigDecimal(3), true);
		balanceService.setConfirmedBalance(GD, Currency.LTC, new BigDecimal(70), true);
		balanceService.setConfirmedBalance(GD, Currency.ETH, new BigDecimal(30), true);
	}
	
	@Test
	public void testInitialBalances() {
		System.out.println(balanceService.getPrettyPrint());
		assertValues("1", balanceService.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceService.getConfirmedAmount(POL, Currency.LTC));
		assertValues("10", balanceService.getConfirmedAmount(POL, Currency.ETH));
		assertValues("2", balanceService.getConfirmedAmount(BIT, Currency.BTC));
		assertValues("60", balanceService.getConfirmedAmount(BIT, Currency.LTC));
		assertValues("20", balanceService.getConfirmedAmount(BIT, Currency.ETH));
		assertValues("3", balanceService.getConfirmedAmount(GD, Currency.BTC));
		assertValues("70", balanceService.getConfirmedAmount(GD, Currency.LTC));
		assertValues("30", balanceService.getConfirmedAmount(GD, Currency.ETH));
		assertValues("1", balanceService.getWorkingAmount(POL, Currency.BTC));
		assertValues("50", balanceService.getWorkingAmount(POL, Currency.LTC));
		assertValues("10", balanceService.getWorkingAmount(POL, Currency.ETH));
		assertValues("2", balanceService.getWorkingAmount(BIT, Currency.BTC));
		assertValues("60", balanceService.getWorkingAmount(BIT, Currency.LTC));
		assertValues("20", balanceService.getWorkingAmount(BIT, Currency.ETH));
		assertValues("3", balanceService.getWorkingAmount(GD, Currency.BTC));
		assertValues("70", balanceService.getWorkingAmount(GD, Currency.LTC));
		assertValues("30", balanceService.getWorkingAmount(GD, Currency.ETH));
	}
	
	@Test
	public void testAdjustedBalances() throws NotEnoughFundsException {
		System.out.println(balanceService.getPrettyPrint());
		balanceService.adiustWorkingAmount(POL, Currency.BTC, bd("0.001"));
		balanceService.adiustWorkingAmount(POL, Currency.LTC, bd("-1.21"));
		balanceService.adiustWorkingAmount(POL, Currency.ETH, bd("3.33"));
		balanceService.adiustWorkingAmount(BIT, Currency.BTC, bd("-1.99"));
		balanceService.adiustWorkingAmount(BIT, Currency.LTC, bd("-60.00"));
		balanceService.adiustWorkingAmount(BIT, Currency.ETH, bd("12"));
		balanceService.adiustWorkingAmount(GD, Currency.BTC, bd("12.00"));
		balanceService.adiustWorkingAmount(GD, Currency.LTC, bd("0.001"));
		balanceService.adiustWorkingAmount(GD, Currency.ETH, bd("-0.001"));
		System.out.println(balanceService.getPrettyPrint());
		assertValues("1", balanceService.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceService.getConfirmedAmount(POL, Currency.LTC));
		assertValues("10", balanceService.getConfirmedAmount(POL, Currency.ETH));
		assertValues("2", balanceService.getConfirmedAmount(BIT, Currency.BTC));
		assertValues("60", balanceService.getConfirmedAmount(BIT, Currency.LTC));
		assertValues("20", balanceService.getConfirmedAmount(BIT, Currency.ETH));
		assertValues("3", balanceService.getConfirmedAmount(GD, Currency.BTC));
		assertValues("70", balanceService.getConfirmedAmount(GD, Currency.LTC));
		assertValues("30", balanceService.getConfirmedAmount(GD, Currency.ETH));
		assertValues("1.001", balanceService.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceService.getWorkingAmount(POL, Currency.LTC));
		assertValues("13.33", balanceService.getWorkingAmount(POL, Currency.ETH));
		assertValues("0.01", balanceService.getWorkingAmount(BIT, Currency.BTC));
		assertValues("0", balanceService.getWorkingAmount(BIT, Currency.LTC));
		assertValues("32", balanceService.getWorkingAmount(BIT, Currency.ETH));
		assertValues("15", balanceService.getWorkingAmount(GD, Currency.BTC));
		assertValues("70.001", balanceService.getWorkingAmount(GD, Currency.LTC));
		assertValues("29.999", balanceService.getWorkingAmount(GD, Currency.ETH));
		balanceService.adiustWorkingAmount(POL, Currency.BTC, bd("0.001"));
		System.out.println(balanceService.getPrettyPrint());
		assertValues("1", balanceService.getConfirmedAmount(POL, Currency.BTC));
		assertValues("1.002", balanceService.getWorkingAmount(POL, Currency.BTC));
		balanceService.adiustWorkingAmount(POL, Currency.BTC, bd("-0.003"));
		System.out.println(balanceService.getPrettyPrint());
		assertValues("1", balanceService.getConfirmedAmount(POL, Currency.BTC));
		assertValues("0.999", balanceService.getWorkingAmount(POL, Currency.BTC));
	}

	@Test(expected=NotEnoughFundsException.class)
	public void testCantWitdrawExcess() throws NotEnoughFundsException {
		balanceService.adiustWorkingAmount(POL, Currency.BTC, bd("-1.0000000000000000001"));
	}

	@Test(expected=NotEnoughFundsException.class)
	public void testThreadSafeCantWitdrawExcess() throws Exception {
		ExecutorService processor = Executors.newFixedThreadPool(11);
		final AtomicReference<Exception> exception = new AtomicReference<Exception>(); 
		final CountDownLatch latch = new CountDownLatch(11); 
		for (int i=0; i<11; i++) {
			processor.submit(() -> {
				try {
					balanceService.adiustWorkingAmount(POL, Currency.BTC, bd("-0.1"));
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
		System.out.println(balanceService.getPrettyPrint());
		assertValues("0", balanceService.getWorkingAmount(POL, Currency.BTC));
		if (exception.get() != null)
			throw exception.get();
	}
	
	@Test
	public void testReconConfirmedBalancesRestoreWorkingBalances() throws NotEnoughFundsException {
		System.out.println(balanceService.getPrettyPrint());
		balanceService.adiustWorkingAmount(POL, Currency.BTC, bd("0.001"));
		balanceService.adiustWorkingAmount(POL, Currency.LTC, bd("-1.21"));
		balanceService.adiustWorkingAmount(GD, Currency.BTC, bd("12.00"));
		System.out.println(balanceService.getPrettyPrint());
		assertValues("1", balanceService.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceService.getConfirmedAmount(POL, Currency.LTC));
		assertValues("3", balanceService.getConfirmedAmount(GD, Currency.BTC));
		assertValues("1.001", balanceService.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceService.getWorkingAmount(POL, Currency.LTC));
		assertValues("15", balanceService.getWorkingAmount(GD, Currency.BTC));
		balanceService.setConfirmedBalance(POL, Currency.BTC, bd("2"), true);
		System.out.println(balanceService.getPrettyPrint());
		assertValues("2", balanceService.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceService.getConfirmedAmount(POL, Currency.LTC));
		assertValues("3", balanceService.getConfirmedAmount(GD, Currency.BTC));
		assertValues("2", balanceService.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceService.getWorkingAmount(POL, Currency.LTC));
		assertValues("15", balanceService.getWorkingAmount(GD, Currency.BTC));
	}

	@Test
	public void testNonReconConfirmedBalancesNotRestoreWorkingBalances() throws NotEnoughFundsException {
		System.out.println(balanceService.getPrettyPrint());
		balanceService.adiustWorkingAmount(POL, Currency.BTC, bd("0.001"));
		balanceService.adiustWorkingAmount(POL, Currency.LTC, bd("-1.21"));
		balanceService.adiustWorkingAmount(GD, Currency.BTC, bd("12.00"));
		System.out.println(balanceService.getPrettyPrint());
		assertValues("1", balanceService.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceService.getConfirmedAmount(POL, Currency.LTC));
		assertValues("3", balanceService.getConfirmedAmount(GD, Currency.BTC));
		assertValues("1.001", balanceService.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceService.getWorkingAmount(POL, Currency.LTC));
		assertValues("15", balanceService.getWorkingAmount(GD, Currency.BTC));
		balanceService.setConfirmedBalance(POL, Currency.BTC, bd("2"), false);
		System.out.println(balanceService.getPrettyPrint());
		assertValues("2", balanceService.getConfirmedAmount(POL, Currency.BTC));
		assertValues("50", balanceService.getConfirmedAmount(POL, Currency.LTC));
		assertValues("3", balanceService.getConfirmedAmount(GD, Currency.BTC));
		assertValues("1.001", balanceService.getWorkingAmount(POL, Currency.BTC));
		assertValues("48.79", balanceService.getWorkingAmount(POL, Currency.LTC));
		assertValues("15", balanceService.getWorkingAmount(GD, Currency.BTC));
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
