package com.kieral.cryptomon;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.kieral.cryptomon.dao.DaoManager;
import com.kieral.cryptomon.service.liquidity.AbstractLiquidityProvider;

//@SessionAttributes({"userObj"})
@Controller
public class CryptoMonController {

//	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
//	@Autowired
//	private DaoManager daoManager;

    @RequestMapping("/")
    public String home(Model model) {
    	return "home";
    }
    
}
	
