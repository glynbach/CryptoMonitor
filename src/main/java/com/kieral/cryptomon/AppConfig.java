package com.kieral.cryptomon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.kieral.cryptomon.dao.DaoManager;
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

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired 
	private Environment env;
	
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
	@Value("${poloniex.maxtrans.second}")
	private int poloniexTransactionsPerSecond;
	@Value("${poloniex.streaming.data.topics}")
	private String poloniexMarketDataTopicsStr;
	@Value("${poloniex.emptypayload.skip}")
	private boolean poloniexSipValidationOnEmptyPayloads;
	@Value("${poloniex.snapshot.required}")
	private boolean poloniexRequiresSnapshot;

	/**
	 * Creates the Poloniex service for market data and execution
	 */
	@Bean
	AbstractLiquidityProvider poloniexService() {
		List<SubscriptionProperties> marketDataTopics = new ArrayList<SubscriptionProperties>();
		Arrays.asList(poloniexMarketDataTopicsStr.split(",")).forEach(topic -> {
			marketDataTopics.add(new SubscriptionProperties.Builder()
					.currencyPair(topic.replaceAll("_", ""))
					.topic(topic).build());
		});;
		ServiceProperties properties = new ServiceProperties.Builder()
											.uri(poloniexUri)
											.marketDataTopics(marketDataTopics)
											.transactionsPerSecond(poloniexTransactionsPerSecond)
											.sipValidationOnEmptyPayloads(poloniexSipValidationOnEmptyPayloads)
											.requiresSnapshot(poloniexRequiresSnapshot)
											.build();
		return new PoloniexService(properties, orderBookManager());		
	}

	@Bean
	OrderBookManager orderBookManager() {
		return new OrderBookManager();
	}

	@Value("${rawDataLoging.enabled}")
	boolean rawDataLoggingEnabled;
	@Value("${dataBufferingLogging.enabled}")
	boolean dataBufferingLoggingEnabled;

	/**
	 * Set additional logging properties
	 */
	void configureLoggingUtils() {
		LoggingUtils.setRawDataLoggingEnabled(rawDataLoggingEnabled);
		LoggingUtils.setDataBufferingLoggingEnabled(dataBufferingLoggingEnabled);
	}
	
	@Bean
	DaoManager daoManager() {
		return new DaoManager();
	}

//	@Bean
//	public SessionFactory sessionFactory() {
//		return new LocalSessionFactoryBuilder(getDataSource())
//				.addAnnotatedClasses(new Class[] { Buyer.class, BuyerNote.class, Seller.class }).buildSessionFactory();
//	}

	@Bean 
	public SimpleDriverDataSource getDataSource() {
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
		dataSource.setUrl("jdbc:mysql://localhost:3306/netrun");
		dataSource.setUsername("netrun");
		dataSource.setPassword("degen789");
		return dataSource;
	}
	
//	@Bean
//	public HibernateTransactionManager hibTransMan() {
//		return new HibernateTransactionManager(sessionFactory());
//	}
	
//	@Bean
//	public JavaMailSender mailSender() {
//		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
//		mailSender.setHost("smtp.cryptorun.net");
//		mailSender.setPort(25);
//		mailSender.setUsername("blacklistdb@cryptorun.net");
//		mailSender.setPassword("degen789");
//		Properties properties = new Properties();
//		properties.setProperty("mail.transport.protocol", "smtp");
//		properties.setProperty("mail.smtp.auth","true");
//		properties.setProperty("mail.debug","true");
//		properties.setProperty("mail.smtp.starttls.enable","true");
//		properties.setProperty("mail.smtp.user","blacklistdb@cryptorun.net");
//		properties.setProperty("mail.smtp.password","degen789");
//		mailSender.setJavaMailProperties(properties);
//		return mailSender;
//	}
	
	@Bean
	public EmbeddedServletContainerFactory servletContainer() {
	    TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
	    if (tomcatAjpEnabled)
	    {
	        Connector ajpConnector = new Connector("AJP/1.3");
	        ajpConnector.setProtocol("AJP/1.3");
	        ajpConnector.setPort(ajpPort);
	        ajpConnector.setSecure(false);
	        ajpConnector.setAllowTrace(false);
	        ajpConnector.setScheme("http");
	        tomcat.addAdditionalTomcatConnectors(ajpConnector);
	    }

	    return tomcat;
	}
}
