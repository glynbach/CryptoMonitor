package com.kieral.cryptomon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.kieral.cryptomon.service.ServiceProperties;
import com.kieral.cryptomon.service.liquidity.AbstractLiquidityProvider;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.liquidity.SubscriptionProperties;
import com.kieral.cryptomon.service.poloniex.PoloniexService;
import com.kieral.cryptomon.service.util.LoggingUtils;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = "com.kieral.cryptomon")
@PropertySource(value = { "classpath:application.properties" })
public class AppConfig extends WebMvcConfigurerAdapter {

	@Value("${tomcat.ajp.port}")
	int ajpPort;

	@Value("${tomcat.ajp.remoteauthentication}")
	String remoteAuthentication;

	@Value("${tomcat.ajp.enabled}")
	boolean tomcatAjpEnabled;
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		super.addResourceHandlers(registry);
		if (!registry.hasMappingForPattern("/static/**")) {
			registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
		}
	}
	
	@Value("${poloniex.push.api}")
	private String poloniexUri;
	@Value("${poloniex.api.key}")
	private String poloniexApiKey;
	@Value("${poloniex.maxtrans.second:5}")
	private int poloniexTransactionsPerSecond;
	@Value("${poloniex.streaming.data.topics}")
	private String poloniexMarketDataTopicsStr;
	@Value("${poloniex.emptypayload.skip:true}")
	private boolean poloniexSipValidationOnEmptyPayloads;
	@Value("${poloniex.snapshot.required:true}")
	private boolean poloniexRequiresSnapshot;
	@Value("${poloniex.use.snapshot.sequence:true}")
	private boolean poloniexUseSnaphotSequence;

	/**
	 * Creates the Poloniex service for market data and execution
	 */
	@Bean
	AbstractLiquidityProvider poloniexService() {
		configureLoggingUtils();
		List<SubscriptionProperties> marketDataTopics = new ArrayList<SubscriptionProperties>();
		Arrays.asList(poloniexMarketDataTopicsStr.split(",")).forEach(topic -> {
			marketDataTopics.add(new SubscriptionProperties.Builder()
					.currencyPair(topic.replaceAll("_", "").trim())
					.topic(topic.trim()).build());
		});;
		ServiceProperties properties = new ServiceProperties.Builder()
											.uri(poloniexUri)
											.marketDataTopics(marketDataTopics)
											.transactionsPerSecond(poloniexTransactionsPerSecond)
											.sipValidationOnEmptyPayloads(poloniexSipValidationOnEmptyPayloads)
											.requiresSnapshot(poloniexRequiresSnapshot)
											.useSnapshotSequence(poloniexUseSnaphotSequence)
											.build();
		return new PoloniexService(properties, orderBookManager());		
	}

	@Bean
	OrderBookManager orderBookManager() {
		return new OrderBookManager();
	}

	@Value("${rawDataLoging.enabled:true}")
	boolean rawDataLoggingEnabled;
	@Value("${dataBufferingLogging.enabled:false}")
	boolean dataBufferingLoggingEnabled;

	/**
	 * Set additional logging properties
	 */
	void configureLoggingUtils() {
		LoggingUtils.setRawDataLoggingEnabled(rawDataLoggingEnabled);
		LoggingUtils.setDataBufferingLoggingEnabled(dataBufferingLoggingEnabled);
	}

	@Bean 
	public SimpleDriverDataSource getDataSource() {
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
		dataSource.setUrl("jdbc:mysql://localhost:3306/cryptomon");
		dataSource.setUsername("cryptorun");
		dataSource.setPassword("cryptodev");
		return dataSource;
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
