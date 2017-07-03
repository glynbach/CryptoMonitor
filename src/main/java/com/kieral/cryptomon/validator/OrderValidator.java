package com.kieral.cryptomon.validator;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import com.kieral.cryptomon.model.general.CurrencyPair;
import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.util.CommonUtils;

@Component
public class OrderValidator implements Validator {

	private final static BigDecimal minimumAmount = new BigDecimal("0.00001");
	private final static BigDecimal minimumPrice = new BigDecimal("0.0000001");
	
	@Autowired 
	ExchangeManagerService exchangeManagerService;
	
	@Override
	public boolean supports(Class<?> clazz) {
		return Order.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "market", "Market is empty", "Market is empty");
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "currencyPairStr", "CurrencyPair is empty", "CurrencyPair is empty");
		ValidationUtils.rejectIfEmpty(errors, "amount", "Amount is empty", "Amount is empty");
		ValidationUtils.rejectIfEmpty(errors, "price", "Price is empty", "Amount is empty");
		ValidationUtils.rejectIfEmpty(errors, "side", "Side is empty", "Side is empty");
		Order order = (Order)target;
		if (!CommonUtils.isAtLeast(order.getAmount(), minimumAmount)) {
			errors.rejectValue("amount", "Amount is too low", "Amount is too low");
		}
		if (!CommonUtils.isAtLeast(order.getPrice(), minimumPrice)) {
			errors.rejectValue("amount", "Price is too low", "Price is too low");
		}
		if (exchangeManagerService.getExchangeStatus(order.getMarket()) != ConnectionStatus.CONNECTED)
			errors.rejectValue("market", "Market is not connected", "Market is not connected");
		if (!exchangeManagerService.isTradingEnabled(order.getMarket()))
			errors.rejectValue("market", "Trading is not enabled for market", "Trading is not enabled for market");
		CurrencyPair currencyPair = exchangeManagerService.getCurrencyPair(order.getMarket(), order.getCurrencyPairStr());
		if (currencyPair == null) 
			errors.rejectValue("currencyPairStr", "Invalid CurrencyPair", "Invalid CurrencyPair");
		else
			order.setCurrencyPair(currencyPair);
		if (order.getOrderStatus() == null)
			order.setOrderStatus(OrderStatus.INITIALISED);
	}
	
}
