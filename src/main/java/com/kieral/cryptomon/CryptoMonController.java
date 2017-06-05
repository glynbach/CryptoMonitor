package com.kieral.cryptomon;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CryptoMonController {

    @RequestMapping("/")
    public String home(Model model) {
    	return "orderBook";
    }
    
}
	
