package com.kieral.cryptomon.model.general;

import java.util.LinkedHashMap;
import java.util.Map;

public class ApiRequest {

	public enum Method {
		GET,
		POST
	}
	
	private final String endPoint;
	private final String requestPath;
	private final Method method;
	private final Map<String, String> postParameters = new LinkedHashMap<String, String>();
	
	public ApiRequest(String endPoint, String requestPath, Method method) {
		this.endPoint = endPoint;
		this.requestPath = requestPath;
		this.method = method;
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

	public Map<String, String> getPostParameters() {
		return postParameters;
	}
	
	public String getPostParametersAsQueryString() {
		StringBuilder sb = new StringBuilder("");
		postParameters.keySet().forEach(key -> {
			sb.append(key).append("=").append(postParameters.get(key)).append("&");
		});
		String rtn = sb.toString(); 
		return rtn.substring(0, rtn.length() == 0 ? 0 : rtn.length() - 1);
	}
	
	@Override
	public String toString() {
		return "ApiRequest [endPoint=" + endPoint + ", requestPath=" + requestPath + ", method=" + method + "]";
	}
	
}
