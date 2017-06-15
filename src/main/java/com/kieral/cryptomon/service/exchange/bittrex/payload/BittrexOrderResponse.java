package com.kieral.cryptomon.service.exchange.bittrex.payload;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kieral.cryptomon.service.rest.OrderResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BittrexOrderResponse implements OrderResponse {

	private String acountId;
	private String uuid;
	private String orderUuid;
	private String exchange;
	private String orderType;
	private String type;
	private BigDecimal quantity;
	private BigDecimal quantityRemaining;
	private BigDecimal limit;
	private BigDecimal commissionPaid;
	private BigDecimal price;
	private BigDecimal pricePerUnit;
	private String opened;
	private String closed;
	private boolean open;
	private boolean cancelInitiated;
	private boolean immediateOrCancel;
	private boolean conditional;
	private String condition;
	private String conditionTarget;

	public String getAcountId() {
		return acountId;
	}

	@JsonProperty("AccountId")
	public void setAcountId(String acountId) {
		this.acountId = acountId;
	}

	public String getUuid() {
		return uuid;
	}

	@JsonProperty("Uuid")
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getOrderUuid() {
		return orderUuid;
	}

	@JsonProperty("OrderUuid")
	public void setOrderUuid(String orderUuid) {
		this.orderUuid = orderUuid;
	}

	public String getExchange() {
		return exchange;
	}

	@JsonProperty("Exchange")
	public void setExchange(String exchange) {
		this.exchange = exchange;
	}

	public String getOrderType() {
		return orderType;
	}

	@JsonProperty("OrderType")
	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public String getType() {
		return type;
	}

	@JsonProperty("Type")
	public void setType(String type) {
		this.type = type;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	@JsonProperty("Quantity")
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getQuantityRemaining() {
		return quantityRemaining;
	}

	@JsonProperty("QuantityRemaining")
	public void setQuantityRemaining(BigDecimal quantityRemaining) {
		this.quantityRemaining = quantityRemaining;
	}

	public BigDecimal getLimit() {
		return limit;
	}

	@JsonProperty("Limit")
	public void setLimit(BigDecimal limit) {
		this.limit = limit;
	}

	public BigDecimal getCommissionPaid() {
		return commissionPaid;
	}

	@JsonProperty("CommissionPaid")
	public void setCommissionPaid(BigDecimal commissionPaid) {
		this.commissionPaid = commissionPaid;
	}

	public BigDecimal getPrice() {
		return price;
	}

	@JsonProperty("Price")
	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public BigDecimal getPricePerUnit() {
		return pricePerUnit;
	}

	@JsonProperty("PricePerUnit")
	public void setPricePerUnit(BigDecimal pricePerUnit) {
		this.pricePerUnit = pricePerUnit;
	}

	public String getOpened() {
		return opened;
	}

	@JsonProperty("Opened")
	public void setOpened(String opened) {
		this.opened = opened;
	}

	public String getClosed() {
		return closed;
	}

	@JsonProperty("Closed")
	public void setClosed(String closed) {
		this.closed = closed;
	}

	public boolean isOpen() {
		return open;
	}

	@JsonProperty("IsOpen")
	public void setOpen(boolean open) {
		this.open = open;
	}

	public boolean isCancelInitiated() {
		return cancelInitiated;
	}

	@JsonProperty("CancelInitiated")
	public void setCancelInitiated(boolean cancelInitiated) {
		this.cancelInitiated = cancelInitiated;
	}

	public boolean isImmediateOrCancel() {
		return immediateOrCancel;
	}

	@JsonProperty("ImmediateOrCancel")
	public void setImmediateOrCancel(boolean immediateOrCancel) {
		this.immediateOrCancel = immediateOrCancel;
	}

	public boolean isConditional() {
		return conditional;
	}

	@JsonProperty("IsConditional")
	public void setConditional(boolean conditional) {
		this.conditional = conditional;
	}

	public String getCondition() {
		return condition;
	}

	@JsonProperty("Condition")
	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getConditionTarget() {
		return conditionTarget;
	}

	@JsonProperty("ConditionTarget")
	public void setConditionTarget(String conditionTarget) {
		this.conditionTarget = conditionTarget;
	}

	@Override
	public String toString() {
		return "BittrexOrderResponse [acountId=" + acountId + ", uuid=" + uuid + ", orderUuid=" + orderUuid
				+ ", exchange=" + exchange + ", orderType=" + orderType + ", type=" + type + ", quantity=" + quantity
				+ ", quantityRemaining=" + quantityRemaining + ", limit=" + limit + ", commissionPaid=" + commissionPaid
				+ ", price=" + price + ", pricePerUnit=" + pricePerUnit + ", opened=" + opened + ", closed=" + closed
				+ ", open=" + open + ", cancelInitiated=" + cancelInitiated + ", immediateOrCancel=" + immediateOrCancel
				+ ", conditional=" + conditional + ", condition=" + condition + ", conditionTarget=" + conditionTarget
				+ "]";
	}

	
}
