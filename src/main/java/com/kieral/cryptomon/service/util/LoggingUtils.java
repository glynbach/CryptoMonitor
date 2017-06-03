package com.kieral.cryptomon.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUtils {

	protected final static Logger dataLogger = LoggerFactory.getLogger("rawdata");

	private static boolean rawDataLoggingEnabled;
	private static boolean dataBufferingLoggingEnabled;
	
	public static boolean isRawDataLoggingEnabled() {
		return rawDataLoggingEnabled;
	}
	public static void setRawDataLoggingEnabled(boolean rawDataLoggingEnabled) {
		LoggingUtils.rawDataLoggingEnabled = rawDataLoggingEnabled;
	}
	public static boolean isDataBufferingLoggingEnabled() {
		return dataBufferingLoggingEnabled;
	}
	public static void setDataBufferingLoggingEnabled(boolean dataBufferingLoggingEnabled) {
		LoggingUtils.dataBufferingLoggingEnabled = dataBufferingLoggingEnabled;
	}

	public static void logRawData(String msg) {
		if (rawDataLoggingEnabled)
			dataLogger.info(msg);
	}

}
