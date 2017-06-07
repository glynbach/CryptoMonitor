package com.kieral.cryptomon.streaming.wamp;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kieral.cryptomon.model.CurrencyPair;
import com.kieral.cryptomon.service.connection.ConnectionStatus;
import com.kieral.cryptomon.service.connection.IStatusListener;
import com.kieral.cryptomon.streaming.StreamingPayload;
import com.kieral.cryptomon.streaming.StreamingProperties;
import com.kieral.cryptomon.streaming.StreamingProvider;

import hu.akarnokd.rxjava.interop.RxJavaInterop;
import io.reactivex.Completable;
import io.reactivex.Observable;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.connection.IWampConnectorProvider;
import ws.wamp.jawampa.transport.netty.NettyWampClientConnectorProvider;

public class WampStreamingProvider implements StreamingProvider {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private WampClient.State connectedState;
	private WampClient client;

	@Override
	public Completable connect(final StreamingProperties connectionProperties, final IStatusListener listener) {
		return Completable.create(completable -> {
			try {
				IWampConnectorProvider connectorProvider = new NettyWampClientConnectorProvider();
				WampClientBuilder builder = new WampClientBuilder();
				builder.withConnectorProvider(connectorProvider)
						.withUri(connectionProperties.getUri())
						.withRealm(connectionProperties.getRealm())
						.withInfiniteReconnects() // TODO: set this as a connectionProperty
						.withReconnectInterval(5, TimeUnit.SECONDS); // TODO: set this as a connectionProperty
				client = builder.build();
				client.statusChanged()
					.subscribe(state -> {
						logger.info("State changed: {}", state);
						if (state instanceof WampClient.DisconnectedState && connectedState instanceof WampClient.ConnectingState) {
		                    if (((WampClient.DisconnectedState) state).disconnectReason() != null) {
		                        completable.onError(((WampClient.DisconnectedState) state).disconnectReason());
		                    } else {
		                        completable.onError(new IllegalStateException("Cannot connect to the exchange."));
		                    }
						}
						connectedState = state;
						informState(connectedState, listener);
						if (state instanceof WampClient.ConnectedState)
							completable.onComplete();
					});
				client.open();
			} catch (Exception e) {
				logger.error("Uncaught exception", e);
				completable.onError(e);
			}
		});
	}

	@Override
	public Observable<StreamingPayload> subscribe(final CurrencyPair currencyPair) {
		if (connectedState != null && connectedState instanceof WampClient.ConnectedState) {
			logger.info("Subscribing to topic {}", currencyPair.getTopic());
			return RxJavaInterop.toV2Observable(client.makeSubscription(currencyPair.getTopic()))
					.map(pubSubData -> {
						return new WampStreamingPayload(pubSubData, currencyPair);
					});
		} else {
			throw new IllegalStateException(String.format("Cannot subscribe in state %s", connectedState));
		}
	}

	private void informState(WampClient.State state, final IStatusListener listener) {
		if (listener != null) {
			if (state instanceof WampClient.DisconnectedState)
				listener.onStatusChange(ConnectionStatus.DISCONNECTED);
			if (state instanceof WampClient.ConnectedState)
				listener.onStatusChange(ConnectionStatus.CONNECTED);
		}
	}

	@Override
	public Completable disconnect() {
		return Completable.create(completable -> {
			try {
				if (client != null)
					client.close();
				completable.onComplete();
			} catch (Exception e) {
				completable.onError(e);
			}

		});
	}
}
