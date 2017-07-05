package com.kieral.cryptomon;

import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kieral.cryptomon.model.trading.Order;
import com.kieral.cryptomon.model.trading.OrderStatus;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.exception.OrderNotExistsException;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.validator.OrderValidator;

@Controller
public class CryptoMonController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	ExchangeManagerService exchangeManagerService;
	@Autowired
	OrderService orderService;
	@Autowired
	OrderValidator orderValidator;

    @RequestMapping("/")
    public String home(Model model) {
    	return "redirect:/orders";
    }

    @RequestMapping("/enableTrading")
    public String secretKey(@RequestParam(value="secretKey", required=true) String secretKey, Model model) {
    	logger.info("Received request to enable trading");
    	List<String> errors = exchangeManagerService.enableTradingAll(secretKey);
    	logger.info("Trading enable response {}", errors);
    	return "redirect:/";
    }

    @RequestMapping("/orderBooks")
    public String orderBooks(Model model) {
    	if (!model.containsAttribute("order"))
    		model.addAttribute("order", new Order());
    	model.addAttribute("exchanges", exchangeManagerService.getEnabledExchangeNames());
    	return "orderBooks";
    }

    @RequestMapping("/orders")
    public String orders(@ModelAttribute("order") Order order, BindingResult result, Model model) {
    	if (!model.containsAttribute("order"))
    		model.addAttribute("order", new Order());
    	model.addAttribute("exchanges", exchangeManagerService.getEnabledExchangeNames());
    	return "orders";
    }

    @RequestMapping("/placeOrder")
    public String placeOrder(@ModelAttribute("order") @Valid Order order, BindingResult result, Model model) {
    	logger.info("Received place order for {}", order);
	    if(result.hasErrors()) {
	    	return "orders";
	    }
	    try {
	    	if (orderService.placeOrder(order) != OrderStatus.OPEN) {
	    		throw new IllegalStateException("Placing order failed");
	    	}
	    	model.addAttribute("info", String.format("Order placed for %s", order.toStringFundamentals()));
	    } catch (Exception e) {
	    	logger.error("Error placing order {}", order, e);
	    	model.addAttribute("error", String.format("Failed to place order for %s", order.toStringFundamentals()));
	    	result.rejectValue("market", "Exception", "Error placing order - " + e.getMessage());
	    	return "orders";
	    }
    	return "redirect:/orders";
    }

    @RequestMapping("/cancelOrder/{market}/{clientOrderId}")
    public String cancelOrder(@PathVariable String market, @PathVariable String clientOrderId, Model model) {
    	logger.info("Received cancel order for clientOrderId {}", clientOrderId);
    	try {
			if (orderService.cancelOrder(market, clientOrderId) == OrderStatus.ERROR) {
				throw new IllegalStateException("Cancel failed");
			}
		} catch (OrderNotExistsException e) {
			logger.error("Received cancel order request {} {} for unrecognised order", market, clientOrderId);
			model.addAttribute("error", String.format("Unrecognised clientOrderId %s for market %s", clientOrderId, market));
		} catch (Exception e) {
	    	logger.error("Error cancelling order for clientOrderId {}", clientOrderId, e);
			model.addAttribute("error", String.format("Error cancelling order for clientOrderId %s - %s", clientOrderId, e.getMessage()));
		}
    	return "redirect:/orders";
    }

    @RequestMapping("/checkOrderStatus/{market}/{clientOrderId}")
    public String checkOrderStatus(@PathVariable String market, @PathVariable String clientOrderId, Model model) {
    	logger.info("Received check order status for clientOrderId {}", clientOrderId);
    	try {
			orderService.checkStatus(market, clientOrderId);
			orderService.requestBalances();
		} catch (OrderNotExistsException e) {
			logger.error("Received check order status {} {} for unrecognised order", market, clientOrderId);
			model.addAttribute("error", String.format("Unrecognised clientOrderId %s for market %s", clientOrderId, market));
		} catch (Exception e) {
	    	logger.error("Error check order status for clientOrderId {}", clientOrderId, e);
			model.addAttribute("error", String.format("Error checking order status for clientOrderId %s - %s", clientOrderId, e.getMessage()));
		}
    	return "redirect:/orders";
    }

    @InitBinder("order")
    public void buyerBinding(WebDataBinder binder) {
    	binder.addValidators(orderValidator);
    } 

}
	
