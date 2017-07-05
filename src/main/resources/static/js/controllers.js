var cryptoApp = angular.module('cryptoApp', ['ngStomp']);
var market = 'all';
var wsUrl = 'https://localhost/marketdata-websocket';
var controllers = {};
var orderScope;
var exchangeScope;
var backOfficeScope;
var arbProspectScope;
cryptoApp.run(function ($stomp, $rootScope) {
	$stomp.connect(wsUrl, {}).then(function (frame) {
		console.log('Stomp connected to ' + wsUrl);
		if (orderScope !== undefined && orderScope !== null) {
			console.log('Subscribing to open orders');
			var subscription = $stomp.subscribe('/topic/openOrders', function (payload, headers, res) {
				orderScope.orders = payload;
				orderScope.$apply(orderScope.orders);
	        });
			$stomp.send("/app/openOrders", market);
		}
		if (exchangeScope !== undefined && exchangeScope !== null) {
			console.log('Subscribing to exchange status');
			var subscription = $stomp.subscribe('/topic/exchangeStatus', function (payload, headers, res) {
				exchangeScope.exchanges = payload;
				exchangeScope.$apply(exchangeScope.exchanges);
	        });
		    $stomp.send("/app/exchangeStatus", market);
		}
		if (backOfficeScope !== undefined && backOfficeScope !== null) {
			console.log('Subscribing to back office updated');
			var subscription = $stomp.subscribe('/topic/backOfficeUpdates', function (payload, headers, res) {
				backOfficeScope.backOfficeUpdates = payload;
				backOfficeScope.lastBackOfficeUpdate = backOfficeScope.backOfficeUpdates[0];
				backOfficeScope.$apply(backOfficeScope.backOfficeUpdates);
				backOfficeScope.$apply(backOfficeScope.lastBackOfficeUpdate);
	        });
		    $stomp.send("/app/exchangeStatus", market);
		}
		if (arbProspectScope !== undefined && arbProspectScope !== null) {
			console.log('Subscribing to arb prospects');
			var subscription = $stomp.subscribe('/topic/arbProspects', function (payload, headers, res) {
				arbProspectScope.arbProspects = payload;
				arbProspectScope.$apply(exchangeScope.arbProspects);
	        });
		    $stomp.send("/app/arbProspectScope", market);
		}
	});
	$rootScope.placeOrder = function placeOrder(market, pair, price, side) {
		$('#placeOrder_market').val(market);
		$('#placeOrder_price').val(price);
		$('#placeOrder_currencyPairStr').val(pair);
		$('#placeOrder_side').val(side);
		$('#placeOrderDialog').dialog('open');
	};
	$rootScope.$on('$destroy', function() {
		console.log('Scope destroy event called');
	});
});
	
controllers.OpenOrderController = function ($stomp, $scope) {
	console.log("Creating orderController");
	orderScope = $scope;
};

controllers.ExchangeController = function ($stomp, $scope) {
	console.log("Creating statusController");
	exchangeScope = $scope;
};

controllers.BackOfficeController = function ($stomp, $scope) {
	console.log("Creating backOfficeController");
	backOfficeScope = $scope;
};

controllers.ArbProspectController = function ($stomp, $scope) {
	console.log("Creating arbProspectController");
	arbProspectScope = $scope;
};

cryptoApp.controller(controllers);