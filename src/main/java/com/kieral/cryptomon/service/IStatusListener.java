package com.kieral.cryptomon.service;

import com.kieral.cryptomon.model.ConnectionStatus;

public interface IStatusListener {

	void onStatusChange(ConnectionStatus status);
	
}
