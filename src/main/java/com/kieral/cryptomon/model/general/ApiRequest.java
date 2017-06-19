package com.kieral.cryptomon.model.general;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kieral.cryptomon.service.exception.ApiRequestException;

public class ApiRequest {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public enum BodyType {
		JSON,
		URLENCODED
	}
	
	private final String endPoint;
	private final String requestPath;
	private final HttpMethod method;
	private final BodyType bodyType;
	private final Map<String, String> postParameters = new LinkedHashMap<String, String>();
	private EnumSet<HttpStatus> acceptableErrorStatuses;

	public ApiRequest(String endPoint, String requestPath, HttpMethod method) {
		this(endPoint, requestPath, method, BodyType.URLENCODED);
	}

	public ApiRequest(String endPoint, String requestPath, HttpMethod method, BodyType bodyType) {
		this(endPoint, requestPath, method, bodyType, null);
	}

	public ApiRequest(String endPoint, String requestPath, HttpMethod method, BodyType bodyType, EnumSet<HttpStatus> acceptableErrorStatuses) {
		this.endPoint = endPoint;
		this.requestPath = requestPath;
		this.method = method;
		this.bodyType = bodyType;
		this.acceptableErrorStatuses = acceptableErrorStatuses;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public HttpMethod getMethod() {
		return method;
	}

	public String getUrl() {
		return endPoint + requestPath;
	}

	public void addPostParameter(String key, String value) {
		if (key == null)
			return;
		postParameters.put(key, value == null ? "" : value);
	}

	public BodyType getBodyType() {
		return bodyType;
	}
	
	public Map<String, String> getPostParameters() {
		return postParameters;
	}
	
	public String getBodyAsString() throws ApiRequestException {
		if (postParameters.size() == 0)
			return "";
		if (bodyType == BodyType.URLENCODED) {
			StringBuilder sb = new StringBuilder("");
			postParameters.keySet().forEach(key -> {
				sb.append(key).append("=").append(postParameters.get(key)).append("&");
			});
			String rtn = sb.toString(); 
			return rtn.substring(0, rtn.length() == 0 ? 0 : rtn.length() - 1);
		} else {
			ObjectNode objectNode = objectMapper.getNodeFactory().objectNode();
			postParameters.keySet().forEach(key -> {
				objectNode.put(key, postParameters.get(key));
			});
			try {
				return objectMapper.writeValueAsString(objectNode);
			} catch (JsonProcessingException e) {
				throw new ApiRequestException(String.format("Exception processing {} to json", postParameters), e);
			}
		}
	}

	public boolean isAcceptableError(HttpStatus statusCode) {
		return acceptableErrorStatuses != null && acceptableErrorStatuses.contains(statusCode);
	}

	@Override
	public String toString() {
		return "ApiRequest [endPoint=" + endPoint + ", requestPath=" + requestPath + ", method=" + method
				+ ", bodyType=" + bodyType + ", postParameters=" + postParameters + ", acceptableErrorStatuses="
				+ acceptableErrorStatuses + "]";
	}
	
}
