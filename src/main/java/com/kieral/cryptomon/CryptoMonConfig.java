package com.kieral.cryptomon;

import java.util.Arrays;

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
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.kieral.cryptomon.service.BackOfficeService;
import com.kieral.cryptomon.service.BackOfficeServiceImpl;
import com.kieral.cryptomon.service.BalanceService;
import com.kieral.cryptomon.service.OrderService;
import com.kieral.cryptomon.service.OrderServiceImpl;
import com.kieral.cryptomon.service.PollingService;
import com.kieral.cryptomon.service.arb.ArbMonitorService;
import com.kieral.cryptomon.service.arb.SimpleArbInspector;
import com.kieral.cryptomon.service.arb.execution.GloballyExclusiveExecutionController;
import com.kieral.cryptomon.service.arb.ArbInspector;
import com.kieral.cryptomon.service.arb.ArbInstructionHandler;
import com.kieral.cryptomon.service.exchange.ExchangeManagerService;
import com.kieral.cryptomon.service.exchange.ExchangePollingService;
import com.kieral.cryptomon.service.exchange.ExchangeService;
import com.kieral.cryptomon.service.exchange.bittrex.BittrexSecurityModule;
import com.kieral.cryptomon.service.exchange.bittrex.BittrexService;
import com.kieral.cryptomon.service.exchange.bittrex.BittrexServiceConfig;
import com.kieral.cryptomon.service.exchange.gdax.GdaxSecurityModule;
import com.kieral.cryptomon.service.exchange.gdax.GdaxService;
import com.kieral.cryptomon.service.exchange.gdax.GdaxServiceConfig;
import com.kieral.cryptomon.service.exchange.poloniex.PoloniexSecurityModule;
import com.kieral.cryptomon.service.exchange.poloniex.PoloniexService;
import com.kieral.cryptomon.service.exchange.poloniex.PoloniexServiceConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookConfig;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.liquidity.OrderBookSanityChecker;
import com.kieral.cryptomon.service.liquidity.OrderBookSanityCheckerImpl;
import com.kieral.cryptomon.service.rest.LoggingRequestInterceptor;
import com.kieral.cryptomon.service.tickstore.TickstoreService;
import com.kieral.cryptomon.service.util.LoggingUtils;
import com.kieral.cryptomon.tickstore.DaoManager;
import com.kieral.cryptomon.tickstore.OrderBookDao;
import com.kieral.cryptomon.tickstore.OrderBookDaoImpl;

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

	@Value("${logging.requestLoggingEnabled:false}")
	boolean requestLoggingEnabled;

	@Value("${logging.responseLoggingEnabled:false}")
	boolean responseLoggingEnabled;

	@Value("${logging.requestResponseFilters:''}")
	String requestResponseFilters;

	@Autowired
	PoloniexServiceConfig poloniexServiceConfig;

	@Autowired
	BittrexServiceConfig bittrexServiceConfig;

	@Autowired
	GdaxServiceConfig gdaxServiceConfig;

	@Autowired
	OrderBookConfig orderBookConfig;

	@Bean
	ExchangeService poloniexService() {
		return new PoloniexService(poloniexServiceConfig, new PoloniexSecurityModule(poloniexServiceConfig));		
	}

	@Bean
	ExchangeService bittrexService() {
		return new BittrexService(bittrexServiceConfig, new BittrexSecurityModule(bittrexServiceConfig));		
	}

	@Bean
	ExchangeService gdaxService() {
		return new GdaxService(gdaxServiceConfig, new GdaxSecurityModule(gdaxServiceConfig));		
	}

	@Bean
	ExchangeManagerService exchangeManagerService() {
		return new ExchangeManagerService(new ExchangeService[]{
				poloniexService(),
				bittrexService(),
				gdaxService()
		});
	}

	@Bean
	PollingService pollingService() {
		return new ExchangePollingService();
	}
	
	@Bean
	OrderService orderService() {
		return new OrderServiceImpl();
	}
	
	@Bean
	TickstoreService tickstoreService() {
		return new TickstoreService();
	}
	
	@Bean
	BalanceService balanceService() {
		BalanceService balanceService = new BalanceService();
		return balanceService;
	}
	
	@Bean
	ArbMonitorService arbMonitorService() {
		return new ArbMonitorService();
	}
	
	@Bean
	ArbInstructionHandler arbInstructionHandler() {
		return new GloballyExclusiveExecutionController();
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
	OrderBookSanityChecker orderBookSanityChecker() {
		return new OrderBookSanityCheckerImpl();
	}
	
	@Bean
	ArbInspector arbInspector() {
		return new SimpleArbInspector();
	}

	@Bean
	BackOfficeService backOfficeService() {
		return new BackOfficeServiceImpl();
	}

	/**
	 * Set additional logging properties
	 */
	@PostConstruct
	void configureLoggingUtils() {
		LoggingUtils.setRawDataLoggingEnabled(rawDataLoggingEnabled);
		LoggingUtils.setDataBufferingLoggingEnabled(dataBufferingLoggingEnabled);
		LoggingUtils.setTickstoreLoggingEnabled(tickstoreLoggingEnabled);
		LoggingUtils.setLogRequestFilters(requestResponseFilters);
		LoggingUtils.setLogRequestsEnabled(requestLoggingEnabled);
		LoggingUtils.setLogResponsesEnabled(responseLoggingEnabled);
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		builder.detectRequestFactory(true);
		RestTemplate template = builder.build();
		configureLoggingUtils();
		if (LoggingUtils.isLogRequestsEnabled() || LoggingUtils.isLogResponsesEnabled()) {
			template.setRequestFactory(new BufferingClientHttpRequestFactory(template.getRequestFactory()));
			template.setInterceptors(Arrays.asList(new ClientHttpRequestInterceptor[]{loggingRequestInterceptor()}));
		}
		return template;
	}
	
	@Bean
	DaoManager daoManager() {
		return new DaoManager();
	}

	@Bean 
	OrderBookDao orderBookDao() {
		return new OrderBookDaoImpl();
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
	
	@Bean
	public ClientHttpRequestInterceptor loggingRequestInterceptor() {
		return new LoggingRequestInterceptor();
	}
	
	@Bean
	public ClientHttpRequestFactory httpComponentsClientHttpRequestFactory() {
		return new HttpComponentsClientHttpRequestFactory();
	}
}
