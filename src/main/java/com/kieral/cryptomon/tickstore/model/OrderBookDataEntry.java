package com.kieral.cryptomon.tickstore.model;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="orderbook_entry")
public class OrderBookDataEntry implements Serializable {

	private static final long serialVersionUID = 805704131659672245L;
	
	private String sessionId;
	private String market;
	private String ccyPair;
	private long snapshotSeq;
	private String side;
	private int index;
	private String baseCurrency;
	private String quotedCurrency;
	private String topic;
	private BigDecimal price;
	private BigDecimal amount;
	private BigDecimal tradingFee;
	private String tradingFeeType;
	private long snapshotTime;
	
	public OrderBookDataEntry() {
	}
	
	public OrderBookDataEntry(String sessionId, String market, String ccyPair, long snapshotSeq, String side, int index, 
			String baseCurrency, String quotedCurrency, String topic, BigDecimal price, BigDecimal amount, 
			BigDecimal tradingFee, String tradingFeeType, long snapshotTime) {
		super();
		this.sessionId = sessionId;
		this.market = market;
		this.ccyPair = ccyPair;
		this.snapshotSeq = snapshotSeq;
		this.side = side;
		this.index = index;
		this.baseCurrency = baseCurrency;
		this.quotedCurrency = quotedCurrency;
		this.topic = topic;
		this.price = price;
		this.amount = amount;
		this.tradingFee = tradingFee;
		this.tradingFeeType = tradingFeeType;
		this.snapshotTime = snapshotTime;
	}

	@Id
	@Column(name="session_id")
	public String getSessionId() {
		return sessionId;
	}
	
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Id
	@Column(name="market")
	public String getMarket() {
		return market;
	}
	
	public void setMarket(String market) {
		this.market = market;
	}
	
	@Id
	@Column(name="ccy_pair")
	public String getCcyPair() {
		return ccyPair;
	}
	
	public void setCcyPair(String ccyPair) {
		this.ccyPair = ccyPair;
	}
	
	@Id
	@Column(name="snapshot_seq")
	public long getSnapshotSeq() {
		return snapshotSeq;
	}
	
	public void setSnapshotSeq(long snapshotSeq) {
		this.snapshotSeq = snapshotSeq;
	}
	
	@Id
	@Column(name="side")
	public String getSide() {
		return side;
	}
	
	public void setSide(String side) {
		this.side = side;
	}
	
	@Id
	@Column(name="index")
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	@Column(name="base_currency")
	public String getBaseCurrency() {
		return baseCurrency;
	}

	public void setBaseCurrency(String baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	@Column(name="quoted_currency")
	public String getQuotedCurrency() {
		return quotedCurrency;
	}

	public void setQuotedCurrency(String quotedCurrency) {
		this.quotedCurrency = quotedCurrency;
	}

	@Column(name="topic")
	public String getTopic() {
		return topic;
	}
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	@Column(name="price")
	public BigDecimal getPrice() {
		return price;
	}
	
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	
	@Column(name="amount")
	public BigDecimal getAmount() {
		return amount;
	}
	
	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}
	
	@Column(name="trading_fee")
	public BigDecimal getTradingFee() {
		return tradingFee;
	}
	
	public void setTradingFee(BigDecimal tradingFee) {
		this.tradingFee = tradingFee;
	}
	
	@Column(name="trading_fee_type")
	public String getTradingFeeType() {
		return tradingFeeType;
	}
	
	public void setTradingFeeType(String tradingFeeType) {
		this.tradingFeeType = tradingFeeType;
	}

	@Column(name="snapshot_time")
	public long getSnapshotTime() {
		return snapshotTime;
	}
	
	public void setSnapshotTime(long snapshotTime) {
		this.snapshotTime = snapshotTime;
	}

	@Override
	public String toString() {
		return "OrderBookDataEntry [sessionId=" + sessionId + ", market=" + market + ", ccyPair=" + ccyPair + ", snapshotSeq=" + snapshotSeq
				+ ", side=" + side + ", index=" + index + ", baseCurrency=" + baseCurrency + ", quotedCurrency="
				+ quotedCurrency + ", topic=" + topic + ", price=" + price + ", amount=" + amount + ", tradingFee="
				+ tradingFee + ", tradingFeeType=" + tradingFeeType + "]";
	}
	
}
