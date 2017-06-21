package com.kieral.cryptomon.model.general;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

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
	
	public enum ResponseErrorAction {
		CANCEL,
		USE_PREVIOUS,
		RAISE_EXCEPTION
	}
	
	private final String endPoint;
	private final String requestPath;
	private final HttpMethod method;
	private final BodyType bodyType;
	private final Map<String, String> postParameters = new LinkedHashMap<String, String>();
	private final ResponseErrorChecker[] responseErrorCheckers;

	public ApiRequest(String endPoint, String requestPath, HttpMethod method) {
		this(endPoint, requestPath, method, BodyType.URLENCODED);
	}

	public ApiRequest(String endPoint, String requestPath, HttpMethod method, BodyType bodyType) {
		this(endPoint, requestPath, method, bodyType, (ResponseErrorChecker[])null);
	}

	public ApiRequest(String endPoint, String requestPath, HttpMethod method, BodyType bodyType, ResponseErrorChecker... responseErrorCheckers) {
		this.endPoint = endPoint;
		this.requestPath = requestPath;
		this.method = method;
		this.bodyType = bodyType;
		this.responseErrorCheckers = responseErrorCheckers;
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

	public ResponseErrorAction checkResponseError(HttpStatus statusCode, String responseBodu) {
		if (responseErrorCheckers != null) {
			for (ResponseErrorChecker responseErrorChecker: responseErrorCheckers) {
				if (statusCode == responseErrorChecker.getStatus()) {
					if (responseErrorChecker.getResponseChecker() != null) {
						return responseErrorChecker.getResponseChecker().apply(responseBodu);
					}
					return ResponseErrorAction.RAISE_EXCEPTION;
				}
			}
		}
		return ResponseErrorAction.RAISE_EXCEPTION;
	}

	@Override
	public String toString() {
		return "ApiRequest [endPoint=" + endPoint + ", requestPath=" + requestPath + ", method=" + method
				+ ", bodyType=" + bodyType + ", postParameters=" + postParameters + ", responseErrorCheckers="
				+ Arrays.toString(responseErrorCheckers) + "]";
	}

	public static class ResponseErrorChecker {
		
		private final HttpStatus status;
		private final Function<String, ResponseErrorAction> responseChecker;
		
		public ResponseErrorChecker(HttpStatus status, Function<String, ResponseErrorAction> responseChecker) {
			this.status = status;
			this.responseChecker = responseChecker;
		}

		public HttpStatus getStatus() {
			return status;
		}

		public Function<String, ResponseErrorAction> getResponseChecker() {
			return responseChecker;
		}

		@Override
		public String toString() {
			return "AcceptableResponseStatus [status=" + status + ", responseChecker=" + responseChecker + "]";
		}
		
	}
	
}
