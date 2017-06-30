package com.kieral.cryptomon.service;

import java.util.List;

import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.service.arb.ArbInstruction;

public interface BackOfficeService {

	void onExecutionCompletion(ArbInstruction instruction, List<Order> longOrders, List<Order> shortOrders, boolean interventionRequired);
	
}
