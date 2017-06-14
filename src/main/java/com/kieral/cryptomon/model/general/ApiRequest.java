package com.kieral.cryptomon.model.general;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ApiRequest {

	public enum Method {
		GET,
		POST
	}

	public enum BodyType {
		JSON,
		URLENCODED
	}
	
	private final String endPoint;
	private final String requestPath;
	private final Method method;
	private final BodyType bodyType;
	private final Map<String, String> postParameters = new LinkedHashMap<String, String>();

	private final ObjectMapper objectMapper = new ObjectMapper();

	public ApiRequest(String endPoint, String requestPath, Method method) {
		this(endPoint, requestPath, method, BodyType.URLENCODED);
	}

	public ApiRequest(String endPoint, String requestPath, Method method, BodyType bodyType) {
		this.endPoint = endPoint;
		this.requestPath = requestPath;
		this.method = method;
		this.bodyType = bodyType;
	}

	public String getEndPoint() {
		return endPoint;
	}

	public String getRequestPath() {
		return requestPath;
	}

	public Method getMethod() {
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
	
	public String getBodyAsString() throws JsonProcessingException {
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
			return objectMapper.writeValueAsString(objectNode);
		}
	}

	@Override
	public String toString() {
		return "ApiRequest [endPoint=" + endPoint + ", requestPath=" + requestPath + ", method=" + method + "]";
	}
	
}
