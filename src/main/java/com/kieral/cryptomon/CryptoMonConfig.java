package com.kieral.cryptomon;

import javax.annotation.PostConstruct;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.kieral.cryptomon.service.exchange.bittrex.BittrexService;
import com.kieral.cryptomon.service.exchange.bittrex.BittrexServiceConfig;
import com.kieral.cryptomon.service.exchange.poloniex.PoloniexService;
import com.kieral.cryptomon.service.exchange.poloniex.PoloniexServiceConfig;
import com.kieral.cryptomon.service.liquidity.BaseService;
import com.kieral.cryptomon.service.liquidity.OrderBookManager;
import com.kieral.cryptomon.service.util.LoggingUtils;

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

	@Value("${logging.datarawDataLogging:true}")
	boolean rawDataLoggingEnabled;

	@Autowired
	PoloniexServiceConfig poloniexServiceConfig;

	@Autowired
	BittrexServiceConfig bittrexServiceConfig;

	@Bean
	BaseService poloniexService() {
		return new PoloniexService(poloniexServiceConfig, orderBookManager());		
	}

	@Bean
	BaseService bittrexService() {
		return new BittrexService(bittrexServiceConfig, orderBookManager());		
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		super.addResourceHandlers(registry);
		if (!registry.hasMappingForPattern("/static/**")) {
			registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
		}
	}
	
	@Bean
	protected OrderBookManager orderBookManager() {
		return new OrderBookManager();
	}

	/**
	 * Set additional logging properties
	 */
	@PostConstruct
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
