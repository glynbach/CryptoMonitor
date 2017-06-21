var cryptoApp = angular.module('cryptoApp', ['ngStomp']);
var market = 'all';
var wsUrl = 'https://localhost/marketdata-websocket';
var controllers = {};
var orderScope;
var exchangeScope;
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
		}
	    $stomp.send("/app/exchangeStatus", market);
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

cryptoApp.controller(controllers);