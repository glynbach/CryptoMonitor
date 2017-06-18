package com.kieral.cryptomon.service.util;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUtils {

	protected final static Logger dataLogger = LoggerFactory.getLogger("rawdata");

	private static boolean rawDataLoggingEnabled;
	private static boolean dataBufferingLoggingEnabled;
	private static boolean tickstoreLoggingEnabled;
	private static boolean logRequestsEnabled;
	private static boolean logResponsesEnabled;
	private static String[] logRequestWhitelistFilterEntries;
	private static String[] logRequestBlacklistFilterEntries;
	
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
	public static boolean isTickstoreLoggingEnabled() {
		return LoggingUtils.tickstoreLoggingEnabled;
	}
	public static void setTickstoreLoggingEnabled(boolean tickstoreLoggingEnabled) {
		LoggingUtils.tickstoreLoggingEnabled = tickstoreLoggingEnabled;
	}
	public static boolean isLogRequestsEnabled() {
		return LoggingUtils.logRequestsEnabled;
	}
	public static boolean isLogRequestsEnabled(String url) {
		return LoggingUtils.logRequestsEnabled && passesFilter(url);
	}
	public static void setLogRequestsEnabled(boolean logRequestsEnabled) {
		LoggingUtils.logRequestsEnabled = logRequestsEnabled;
	}
	public static boolean isLogResponsesEnabled() {
		return LoggingUtils.logResponsesEnabled;
	}
	public static boolean isLogResponsesEnabled(String url) {
		return LoggingUtils.logResponsesEnabled & passesFilter(url);
	}
	public static void setLogResponsesEnabled(boolean logResponsesEnabled) {
		LoggingUtils.logResponsesEnabled = logResponsesEnabled;
	}
	public static void setLogRequestFilters(String logRequestFilters) {
		if (logRequestFilters == null || logRequestFilters.trim().length() == 0) {
			logRequestBlacklistFilterEntries = new String[]{};
			logRequestWhitelistFilterEntries = new String[]{};
		}
		else {
			List<String> blackList = new ArrayList<String>();
			List<String> whiteList = new ArrayList<String>();
			for (String entry : logRequestFilters.split(",")) {
				if (entry.trim().startsWith("!"))
					blackList.add(entry.trim());
				else
					whiteList.add(entry.trim());
			}
			logRequestBlacklistFilterEntries = blackList.toArray(new String[blackList.size()]);
			logRequestWhitelistFilterEntries = whiteList.toArray(new String[whiteList.size()]);
		}
	}

	private static boolean passesFilter(String url) {
		for (String bl : logRequestBlacklistFilterEntries) {
			if (url.contains(bl.substring(1)))
				return false;
		}
		if (logRequestWhitelistFilterEntries.length == 0)
			return true;
		else {
			for (String wl : logRequestWhitelistFilterEntries) {
				if (url.contains(wl))
					return true;
			}
			return false;
		}
	}

	public static void logRawData(String msg) {
		if (rawDataLoggingEnabled)
			dataLogger.info(msg);
	}

}
