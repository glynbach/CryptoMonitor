package com.kieral.cryptomon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kieral.cryptomon.service.exchange.ExchangeManagerService;

@Controller
public class CryptoMonController {

	@Autowired
	ExchangeManagerService exchangeManagerService;
	
    @RequestMapping("/")
    public String home(Model model) {
    	return "home";
    }

    @RequestMapping("/unlockTrading")
    public String secretKey(@RequestParam(value="secretKey", required=true) String secretKey, Model model) {
    	exchangeManagerService.getEnabledExchanges().forEach(service -> {
    		service.unlockTrading(secretKey);
    	});
    	return "home";
    }

}
	
