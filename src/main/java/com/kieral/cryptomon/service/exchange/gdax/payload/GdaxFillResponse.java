package com.kieral.cryptomon.service.exchange.gdax.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kieral.cryptomon.service.exchange.gdax.GdaxServiceConfig;
import com.kieral.cryptomon.service.rest.TradeResponse;
import com.kieral.cryptomon.service.util.CommonUtils;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GdaxFillResponse implements TradeResponse {

	private String tradeId;
	private String productId;
	private BigDecimal price;
	private BigDecimal size;
	private String orderId;
	private String createdAt;
	private String liquidity;
	private BigDecimal fee;
	private boolean settled;
	private String side;
	
	public String getTradeId() {
		return tradeId;
	}

	@JsonProperty("trade_id")
	public void setTradeId(String tradeId) {
		this.tradeId = tradeId;
	}

	public String getProductId() {
		return productId;
	}

	@JsonProperty("product_id")
	public void setProductId(String productId) {
		this.productId = productId;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getSize() {
		return size;
	}

	public void setSize(BigDecimal size) {
		this.size = size;
	}

	public String getOrderId() {
		return orderId;
	}

	@JsonProperty("order_id")
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getCreatedAt() {
		return createdAt;
	}

	@JsonProperty("created_at")
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}

	public String getLiquidity() {
		return liquidity;
	}

	public void setLiquidity(String liquidity) {
		this.liquidity = liquidity;
	}

	public BigDecimal getFee() {
		return fee;
	}

	public void setFee(BigDecimal fee) {
		this.fee = fee;
	}

	public boolean isSettled() {
		return settled;
	}

	public void setSettled(boolean settled) {
		this.settled = settled;
	}

	public String getSide() {
		return side;
	}

	public void setSide(String side) {
		this.side = side;
	}

	@Override
	public String toString() {
		return "GdaxFillResponse [tradeId=" + tradeId + ", productId=" + productId + ", price=" + price + ", size="
				+ size + ", orderId=" + orderId + ", createdAt=" + createdAt + ", liquidity=" + liquidity + ", fee="
				+ fee + ", settled=" + settled + ", side=" + side + "]";
	}

	@Override
	public BigDecimal getRate() {
		return price;
	}

	@Override
	public BigDecimal getAmount() {
		return size;
	}

	@Override
	public boolean isFeeInQuotedCurrency() {
		return true;
	}

	@Override
	public long getTradeTime() {
		return CommonUtils.getMillis(createdAt, GdaxServiceConfig.dateTimeFormatter, 0);
	}
	
}
