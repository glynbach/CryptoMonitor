package com.kieral.cryptomon;

import java.math.BigDecimal;

import javax.annotation.PostConstruct;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.kieral.cryptomon.model.general.Currency;
import com.kieral.cryptomon.service.BalanceHandler;
import com.kieral.cryptomon.service.arb.ArbInstruction;
import com.kieral.cryptomon.service.arb.ArbMonitorService;
import com.kieral.cryptomon.service.arb.BasicArbExaminer;
import com.kieral.cryptomon.service.arb.IArbExaminer;
import com.kieral.cryptomon.service.arb.IArbInstructionHandler;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.exchange.IExchangeService;
import com.kieral.cryptomon.service.exchange.bittrex.BittrexService;
import com.kieral.cryptomon.service.exchange.bittrex.BittrexServiceConfig;
import com.kieral.cryptomon.service.exchange.gdax.GdaxService;
import com.kieral.cryptomon.service.exchange.gdax.GdaxServiceConfig;
import com.kieral.cryptomon.service.exchange.poloniex.PoloniexService;
import com.kieral.cryptomon.service.exchange.poloniex.PoloniexServiceConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.tickstore.TickstoreService;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.tickstore.DaoManager;
import com.kieral.cryptomon.tickstore.IOrderBookDao;
import com.kieral.cryptomon.tickstore.OrderBookDao;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = "com.kieral.cryptomon")
@PropertySource(value = { "classpath:application.yaml" })
public class CryptoMonConfig extends WebMvcConfigurerAdapter {

	@Value("${tomcat.ajp.port}")
	int ajpPort;

	@Value("${tomcat.ajp.remoteauthentication}")
	String remoteAuthentication;

	@Value("${tomcat.ajp.enabled}")
	boolean tomcatAjpEnabled;

	@Value("${logging.dataBufferingLogging:false}")
	boolean dataBufferingLoggingEnabled;

	@Value("${logging.rawDataLogging:true}")
	boolean rawDataLoggingEnabled;

	@Value("${logging.tickstoreLoggingEnabled:false}")
	boolean tickstoreLoggingEnabled;
	
	@Autowired
	PoloniexServiceConfig poloniexServiceConfig;

	@Autowired
	BittrexServiceConfig bittrexServiceConfig;

	@Autowired
	GdaxServiceConfig gdaxServiceConfig;

	@Autowired
	OrderBookConfig orderBookConfig;

	@Bean
	IExchangeService poloniexService() {
		return new PoloniexService(poloniexServiceConfig);		
	}

	@Bean
	IExchangeService bittrexService() {
		return new BittrexService(bittrexServiceConfig);		
	}

	@Bean
	IExchangeService gdaxService() {
		return new GdaxService(gdaxServiceConfig);		
	}

	@Bean
	ExchangeManagerService exchangeManagerService() {
		return new ExchangeManagerService(new IExchangeService[]{
				poloniexService(),
				bittrexService(),
				gdaxService()
		});
	}
	
	@Bean
	TickstoreService tickstoreService() {
		return new TickstoreService();
	}
	
	@Bean
	BalanceHandler balanceHandler() {
		BalanceHandler balanceHandler = new BalanceHandler();
		// TODO: implement - adding some now for testing
		balanceHandler.setConfirmedBalance("poloniex", Currency.BTC, new BigDecimal(1), true);
		balanceHandler.setConfirmedBalance("poloniex", Currency.LTC, new BigDecimal(50), true);
		balanceHandler.setConfirmedBalance("poloniex", Currency.ETH, new BigDecimal(10), true);
		balanceHandler.setConfirmedBalance("bittrex", Currency.BTC, new BigDecimal(1), true);
		balanceHandler.setConfirmedBalance("bittrex", Currency.LTC, new BigDecimal(50), true);
		balanceHandler.setConfirmedBalance("bittrex", Currency.ETH, new BigDecimal(10), true);
		balanceHandler.setConfirmedBalance("gdax", Currency.BTC, new BigDecimal(1), true);
		balanceHandler.setConfirmedBalance("gdax", Currency.LTC, new BigDecimal(50), true);
		balanceHandler.setConfirmedBalance("gdax", Currency.ETH, new BigDecimal(10), true);
		return balanceHandler;
	}
	
	@Bean
	ArbMonitorService arbMonitorService() {
		return new ArbMonitorService();
	}
	
	@Bean
	IArbInstructionHandler arbInstructionHandler() {
		// TODO: implement this
		return new IArbInstructionHandler() {
			@Override
			public void onArbInstruction(ArbInstruction instruction) {
			}
		};
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		super.addResourceHandlers(registry);
		if (!registry.hasMappingForPattern("/static/**")) {
			registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
		}
	}
	
	@Bean
	OrderBookManager orderBookManager() {
		return new OrderBookManager();
	}

	@Bean
	IArbExaminer arbExaminer() {
		return new BasicArbExaminer();
	}
	/**
	 * Set additional logging properties
	 */
	@PostConstruct
	void configureLoggingUtils() {
		LoggingUtils.setRawDataLoggingEnabled(rawDataLoggingEnabled);
		LoggingUtils.setDataBufferingLoggingEnabled(dataBufferingLoggingEnabled);
		LoggingUtils.setTickstoreLoggingEnabled(tickstoreLoggingEnabled);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}
	
	@Bean
	DaoManager daoManager() {
		return new DaoManager();
	}

	@Bean 
	IOrderBookDao orderBookDao() {
		return new OrderBookDao();
	}

	@Bean
	public EmbeddedServletContainerFactory servletContainer() {
	    TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
	    if (tomcatAjpEnabled)
	    {
	        Connector ajpConnector = new Connector("AJP/1.3");
	        ajpConnector.setPort(ajpPort);
	        ajpConnector.setSecure(false);
	        ajpConnector.setAllowTrace(false);
	        ajpConnector.setScheme("http");
	        tomcat.addAdditionalTomcatConnectors(ajpConnector);
	    }

	    return tomcat;
	}
}
