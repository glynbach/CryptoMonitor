package com.kieral.cryptomon.service;

public interface PollListener {

	void polled();

	void polledException(Exception e);
}
