package com.kieral.cryptomon;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kieral.cryptomon.service.exchange.ExchangeManagerService;

@Controller
public class CryptoMonController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	ExchangeManagerService exchangeManagerService;
	
    @RequestMapping("/")
    public String home(Model model) {
    	return "home";
    }

    @RequestMapping("/unlockTrading")
    public String secretKey(@RequestParam(value="secretKey", required=true) String secretKey, Model model) {
    	logger.info("Received request to unlock trading");
    	List<String> errors = exchangeManagerService.unlockTradingAll(secretKey);
    	logger.info("Trading unlock response {}", errors);
    	//TODO: redirect this back to home
    	return "home";
    }

}
	
